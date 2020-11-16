package com.golovkin;

import java.io.IOException;
import java.util.Random;

public class Main {
    //region Constants
    private static final char ALIVE_CELL_CHAR = '█';
    private static final char DEAD_CELL_CHAR = ' ';

    private static final int MAP_WIDTH = 20;
    private static final int MAP_HEIGHT = 20;

    private static final int PAUSE_BETWEEN_GENERATIONS_IN_MILLIS = 100;

    private static final int NEIGHBOURS_COUNT_IF_UNDERPOPULATED = 2;
    private static final int NEIGHBOURS_COUNT_IF_OVERPOPULATED = 3;
    private static final int NEIGHBOURS_COUNT_IF_NEW_SHOULD_BORN = 3;
    private static final String GAME_OVER_MESSAGE = "GAME OVER";

    private static ProcessBuilder windowsConsoleCleanerProcessBuilder;
    private static boolean[] adjacentNeighbours = new boolean[8];
    //endregion

    // При запуске на Windows через консоль она будет очищаться при каждом новом поколении
    public static void main(String[] args) {
        try {
            checkOs();
            checkConstants();
            gameLoop();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    //region Checks
    private static void checkOs() {
        final String os = System.getProperty("os.name");

        if (os.contains("Windows")) {
            windowsConsoleCleanerProcessBuilder = new ProcessBuilder("cmd", "/c", "cls").inheritIO();
        }
    }

    private static void checkConstants() {
        int[] constants = {MAP_WIDTH, MAP_HEIGHT, PAUSE_BETWEEN_GENERATIONS_IN_MILLIS,
                NEIGHBOURS_COUNT_IF_UNDERPOPULATED, NEIGHBOURS_COUNT_IF_OVERPOPULATED, NEIGHBOURS_COUNT_IF_NEW_SHOULD_BORN};

        for (int constant : constants) {
            if (constant < 0) {
                throw new IllegalArgumentException("Значение констант должно быть > 0.");
            }
        }

        if (MAP_WIDTH < 9 || MAP_HEIGHT < 9) {
            throw new IllegalArgumentException("Неверные размеры карты.");
        }

        if (NEIGHBOURS_COUNT_IF_UNDERPOPULATED > NEIGHBOURS_COUNT_IF_OVERPOPULATED) {
            throw new IllegalArgumentException("Число \"одиночества\" не может быть больше числа \"перенаселенности\".");
        }

        if (NEIGHBOURS_COUNT_IF_NEW_SHOULD_BORN < NEIGHBOURS_COUNT_IF_UNDERPOPULATED || NEIGHBOURS_COUNT_IF_NEW_SHOULD_BORN > NEIGHBOURS_COUNT_IF_OVERPOPULATED) {
            throw new IllegalArgumentException("Новая жизнь никогда не появится. Измените соответствующую константу.");
        }
    }
    //endregion

    private static void gameLoop() throws IOException, InterruptedException {
        boolean[][] currentGenerationMap = new boolean[MAP_HEIGHT][MAP_WIDTH];
        initializeMap(currentGenerationMap);

        boolean[][] nextGenerationMap = new boolean[MAP_HEIGHT][MAP_WIDTH];

        boolean[][] buf;
        while (true) {
            displayMap(currentGenerationMap);
            copyMapContent(currentGenerationMap, nextGenerationMap);

            computeNextGeneration(currentGenerationMap, nextGenerationMap);

            if (isGameOver(currentGenerationMap, nextGenerationMap)) {
                System.out.println(GAME_OVER_MESSAGE);
                break;
            }

            buf = currentGenerationMap;
            currentGenerationMap = nextGenerationMap;
            nextGenerationMap = buf;

            Thread.sleep(PAUSE_BETWEEN_GENERATIONS_IN_MILLIS);
        }
    }

    //region Game logic
    private static void initializeMap(boolean[][] map) {
        Random random = new Random();
        for (int i = 1; i < map.length - 1; i++) {
            for (int j = 1; j < map[0].length - 1; j++) {
                map[i][j] = random.nextBoolean();
            }
        }
    }

    private static void copyMapContent(boolean[][] sourceMap, boolean[][] destinationMap) {
        for (int i = 0; i < sourceMap.length; i++) {
            System.arraycopy(sourceMap[i], 0, destinationMap[i], 0, sourceMap[0].length);
        }
    }

    private static void computeNextGeneration(boolean[][] currentGenerationMap, boolean[][] nextGenerationMap) {
        for (int i = 1; i < currentGenerationMap.length - 1; i++) {
            for (int j = 1; j < currentGenerationMap[0].length - 1; j++) {
                if (isUnderpopulated(i, j, currentGenerationMap) || isOverpopulated(i, j, currentGenerationMap)) {
                    nextGenerationMap[i][j] = false;
                } else if (shouldBorn(i, j, currentGenerationMap)) {
                    nextGenerationMap[i][j] = true;
                }
            }
        }
    }

    private static boolean isUnderpopulated(int i, int j, boolean[][] map) {
        int counter = getAliveAdjacentNeighboursCount(i, j, map);

        return map[i][j] && counter < NEIGHBOURS_COUNT_IF_UNDERPOPULATED;
    }

    private static boolean isOverpopulated(int i, int j, boolean[][] map) {
        int counter = getAliveAdjacentNeighboursCount(i, j, map);

        return map[i][j] && counter > NEIGHBOURS_COUNT_IF_OVERPOPULATED;
    }

    private static boolean shouldBorn(int i, int j, boolean[][] map) {
        int counter = getAliveAdjacentNeighboursCount(i, j, map);

        return !map[i][j] && counter == NEIGHBOURS_COUNT_IF_NEW_SHOULD_BORN;
    }

    private static int getAliveAdjacentNeighboursCount(int i, int j, boolean[][] map) {
        populateAdjacentNeigboursArray(i, j, map);

        int aliveAdjacentNeighbourCount = 0;

        for (int k = 0; k < adjacentNeighbours.length; k++) {
            if (adjacentNeighbours[k]) {
                aliveAdjacentNeighbourCount++;
            }
        }

        return aliveAdjacentNeighbourCount;
    }

    private static void populateAdjacentNeigboursArray(int i, int j, boolean[][] map) {
        adjacentNeighbours[0] = map[i - 1][j - 1];
        adjacentNeighbours[1] = map[i][j - 1];
        adjacentNeighbours[2] = map[i + 1][j - 1];
        adjacentNeighbours[3] = map[i - 1][j];
        adjacentNeighbours[4] = map[i + 1][j];
        adjacentNeighbours[5] = map[i - 1][j + 1];
        adjacentNeighbours[6] = map[i][j + 1];
        adjacentNeighbours[7] = map[i + 1][j + 1];
    }

    private static boolean isGameOver(boolean[][] currentGenerationMap, boolean[][] nextGenerationMap) {
        return areThereNoAliveCells(nextGenerationMap) || doesNextConfigurationRepeatCurrent(currentGenerationMap, nextGenerationMap);
    }

    private static boolean doesNextConfigurationRepeatCurrent(boolean[][] currentGenerationMap, boolean[][] nextGenerationMap) {
        for (int i = 0; i < currentGenerationMap.length; i++) {
            for (int j = 0; j < currentGenerationMap[0].length; j++) {
                if (currentGenerationMap[i][j] != nextGenerationMap[i][j]) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean areThereNoAliveCells(boolean[][] map) {
        for (int i = 1; i < map.length - 1; i++) {
            for (int j = 1; j < map[0].length - 1; j++) {
                if (map[i][j]) {
                    return false;
                }
            }
        }

        return true;
    }
    //endregion

    //region IO methods
    private static void displayMap(boolean[][] array) throws IOException, InterruptedException {
        tryClearConsole();
        printGeneration(array);
    }

    private static void tryClearConsole() throws InterruptedException, IOException {
        if (windowsConsoleCleanerProcessBuilder != null) {
            windowsConsoleCleanerProcessBuilder.start().waitFor();
        }
    }

    private static void printGeneration(boolean[][] array) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 1; i < array.length - 1; i++) {
            for (int j = 1; j < array[0].length - 1; j++) {
                if (array[i][j]) {
                    stringBuilder.append(ALIVE_CELL_CHAR);
                } else {
                    stringBuilder.append(DEAD_CELL_CHAR);
                }
            }
            stringBuilder.append("\n");
        }

        System.out.println(stringBuilder);
    }
    //endregion
}
