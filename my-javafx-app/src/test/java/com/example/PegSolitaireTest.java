package com.example;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PegSolitaireTest {

    @Test
    void eachBoardTypeCreatesExpectedInitialPosition() {
        assertInitialBoard(PegSolitaire.BoardType.ENGLISH_33, 7, 7, 32, 3, 3);
        assertInitialBoard(PegSolitaire.BoardType.DIAMOND_41, 9, 9, 40, 4, 4);
        assertInitialBoard(PegSolitaire.BoardType.HEXAGON_21, 5, 7, 20, 2, 3);
    }

    @Test
    void creatingBoardAgainDoesNotReuseMutableGrid() {
        int[][] first = PegSolitaire.BoardType.ENGLISH_33.createInitialGrid();
        int[][] second = PegSolitaire.BoardType.ENGLISH_33.createInitialGrid();

        assertNotSame(first, second);
        assertNotSame(first[0], second[0]);
        first[2][2] = 0;

        assertEquals(1, second[2][2]);
        assertEquals(1, PegSolitaire.BoardType.ENGLISH_33.createInitialGrid()[2][2]);
    }

    @Test
    void acceptsValidHorizontalAndVerticalJumps() {
        int[][] horizontal = {
            {1, 1, 2}
        };
        int[][] vertical = {
            {1},
            {1},
            {2}
        };

        assertTrue(PegSolitaire.isValidMove(horizontal, 0, 0, 0, 2));
        assertTrue(PegSolitaire.isValidMove(vertical, 0, 0, 2, 0));
    }

    @Test
    void rejectsMovesThatDoNotFollowPegSolitaireRules() {
        int[][] board = {
            {1, 1, 2},
            {1, 1, 2},
            {2, 2, 2}
        };
        int[][] occupiedDestination = {
            {1, 1, 1}
        };

        assertFalse(PegSolitaire.isValidMove(board, 0, 0, 1, 1), "diagonal jump");
        assertFalse(PegSolitaire.isValidMove(board, 0, 0, 0, 1), "one-space move");
        assertFalse(PegSolitaire.isValidMove(occupiedDestination, 0, 0, 0, 2), "destination is occupied");
        assertFalse(PegSolitaire.isValidMove(board, 0, 2, 0, 0), "source has no peg");
        assertFalse(PegSolitaire.isValidMove(board, 0, 0, 0, 3), "destination is outside the board");
        assertFalse(PegSolitaire.isValidMove(board, 0, 0, -2, 0), "destination is outside the board");
    }

    @Test
    void applyingValidMoveUpdatesThreeHolesAndReducesPegCount() {
        int[][] board = {
            {1, 1, 2}
        };

        assertTrue(PegSolitaire.applyMove(board, 0, 0, 0, 2));

        assertArrayEquals(new int[]{2, 2, 1}, board[0]);
        assertEquals(1, PegSolitaire.countRemainingPegs(board));
    }

    @Test
    void applyingInvalidMoveLeavesBoardUnchanged() {
        int[][] board = {
            {1, 2, 2}
        };
        int[][] original = {
            {1, 2, 2}
        };

        assertFalse(PegSolitaire.applyMove(board, 0, 0, 0, 2));

        assertArrayEquals(original[0], board[0]);
    }

    @Test
    void reportsWhetherAnyLegalMovesRemain() {
        int[][] movable = {
            {1, 1, 2}
        };
        int[][] blocked = {
            {1, 2, 1}
        };

        assertTrue(PegSolitaire.hasAnyValidMoves(movable));
        assertFalse(PegSolitaire.hasAnyValidMoves(blocked));
    }

    @Test
    void startNewGameResetScoreAndTime() {
        PegSolitaire.GameState gameState = new PegSolitaire.GameState();
        gameState.startNewGame(PegSolitaire.BoardType.ENGLISH_33);

        assertTrue(PegSolitaire.applyMove(gameState.getGrid(), 3, 1, 3, 3));
        gameState.addScore(10);
        gameState.incrementSecondsElapsed();

        gameState.startNewGame(PegSolitaire.BoardType.DIAMOND_41);

        assertEquals(0, gameState.getScore());
        assertEquals(0, gameState.getSecondsElapsed());

        int[][] expectedGrid = PegSolitaire.BoardType.DIAMOND_41.createInitialGrid();
        int[][] actualGrid = gameState.getGrid();
        assertEquals(expectedGrid.length, actualGrid.length);
        for (int row = 0; row < expectedGrid.length; row++) {
            assertArrayEquals(expectedGrid[row], actualGrid[row]);
        }
    }

    @Test
    void cancelRestartKeepCurrentGameRequestConfirm() {
        PegSolitaire.GameState gameState = new PegSolitaire.GameState();
        gameState.startNewGame(PegSolitaire.BoardType.ENGLISH_33);
        assertTrue(PegSolitaire.applyMove(gameState.getGrid(), 3, 1, 3, 3));
        gameState.addScore(10);
        gameState.incrementSecondsElapsed();

        int[][] gridBeforeRestart = copyGrid(gameState.getGrid());
        int scoreBeforeRestart = gameState.getScore();
        int timeBeforeRestart = gameState.getSecondsElapsed();
        boolean[] confirmationRequested = {false};

        boolean restarted = gameState.restartIfConfirmed(PegSolitaire.BoardType.ENGLISH_33, () -> {
            confirmationRequested[0] = true;
            return false;
        });

        assertTrue(confirmationRequested[0]);
        assertFalse(restarted);
        assertEquals(scoreBeforeRestart, gameState.getScore());
        assertEquals(timeBeforeRestart, gameState.getSecondsElapsed());
        assertGridEquals(gridBeforeRestart, gameState.getGrid());
    }

    @Test
    void confirmingRestartResetsCurrentGameAndRequestsConfirmation() {
        PegSolitaire.GameState gameState = new PegSolitaire.GameState();
        gameState.startNewGame(PegSolitaire.BoardType.ENGLISH_33);
        assertTrue(PegSolitaire.applyMove(gameState.getGrid(), 3, 1, 3, 3));
        gameState.addScore(10);
        gameState.incrementSecondsElapsed();

        boolean[] confirmationRequested = {false};
        boolean restarted = gameState.restartIfConfirmed(PegSolitaire.BoardType.ENGLISH_33, () -> {
            confirmationRequested[0] = true;
            return true;
        });

        assertTrue(confirmationRequested[0]);
        assertTrue(restarted);
        assertEquals(0, gameState.getScore());
        assertEquals(0, gameState.getSecondsElapsed());
        assertGridEquals(
            PegSolitaire.BoardType.ENGLISH_33.createInitialGrid(),
            gameState.getGrid()
        );
    }

    @Test
    void validHorizontalMoveMovesPegAndRemovesJumpedPeg() {
        // Arrange: peg, peg, empty hole
        int[][] board = {
            {1, 1, 2}
    };

        // Act: jump from column 0 to column 2
        boolean moved = PegSolitaire.applyMove(board, 0, 0, 0, 2);

        // Assert: starting hole and jumped hole are empty;
        // destination contains the moved peg.
        assertTrue(moved);
        assertArrayEquals(new int[]{2, 2, 1}, board[0]);
        assertEquals(1, PegSolitaire.countRemainingPegs(board));
    }

    @Test
    void validVerticalMoveMovesPegAndRemovesJumpedPeg() {
        // Arrange: peg, peg, empty hole vertically
        int[][] board = {
            {1},
            {1},
            {2}
        };

        // Act: jump from row 0 to row 2
        boolean moved = PegSolitaire.applyMove(board, 0, 0, 2, 0);

        // Assert
        assertTrue(moved);
        assertEquals(2, board[0][0], "Starting hole should be empty");
        assertEquals(2, board[1][0], "Jumped peg should be removed");
        assertEquals(1, board[2][0], "Destination should contain the moved peg");
        assertEquals(1, PegSolitaire.countRemainingPegs(board));
    }

    @Test
    void finalMoveWithOnePegReportsVictory() {
        int[][] board = {
            {1, 1, 2}
        };
        assertTrue(PegSolitaire.applyMove(board, 0, 0, 0, 2));

        Optional<PegSolitaire.GameOverResult> result =
            PegSolitaire.getGameOverResult(board, 10, "00:01");

        assertTrue(result.isPresent());
        assertEquals(PegSolitaire.GameOutcome.WON, result.get().outcome());
        assertEquals(1, result.get().remainingPegs());
        assertTrue(result.get().title().toLowerCase().contains("win"));
        assertTrue(result.get().message().contains("1 peg remaining"));
        assertTrue(result.get().message().contains("Final Score: 10"));
        assertTrue(result.get().message().contains("Time: 00:01"));
        assertEquals(List.of("Restart", "Exit"), result.get().actions());
    }

    @Test
    void finalMoveWithMultiplePegsReportsDefeatAndPegCount() {
        int[][] board = {
            {1, 1, 2, 0, 1}
        };
        assertTrue(PegSolitaire.applyMove(board, 0, 0, 0, 2));

        Optional<PegSolitaire.GameOverResult> result =
            PegSolitaire.getGameOverResult(board, 10, "00:01");

        assertTrue(result.isPresent());
        assertEquals(PegSolitaire.GameOutcome.LOST, result.get().outcome());
        assertEquals(2, result.get().remainingPegs());
        assertEquals("Game Over", result.get().title());
        assertTrue(result.get().message().contains("2 pegs remaining"));
        assertTrue(result.get().message().contains("Final Score: 10"));
        assertTrue(result.get().message().contains("Time: 00:01"));
        assertEquals(List.of("Restart", "Exit"), result.get().actions());
    }

    @Test
    void gameOverIsNotReportedWhileValidMovesRemain() {
        int[][] board = {
            {1, 1, 2}
        };

        assertEquals(
            Optional.empty(),
            PegSolitaire.getGameOverResult(board, 0, "00:00")
        );
    }

    private static int[][] copyGrid(int[][] grid) {
        int[][] copy = new int[grid.length][];
        for (int row = 0; row < grid.length; row++) {
            copy[row] = grid[row].clone();
        }
        return copy;
    }

    private static void assertGridEquals(int[][] expected, int[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int row = 0; row < expected.length; row++) {
            assertArrayEquals(expected[row], actual[row]);
        }
    }

    private static void assertInitialBoard(
        PegSolitaire.BoardType boardType,
        int expectedRows,
        int expectedColumns,
        int expectedPegs,
        int emptyRow,
        int emptyColumn
    ) {
        int[][] grid = boardType.createInitialGrid();

        assertEquals(expectedRows, grid.length);
        for (int[] row : grid) {
            assertEquals(expectedColumns, row.length);
        }
        assertEquals(2, grid[emptyRow][emptyColumn]);
        assertEquals(expectedPegs, PegSolitaire.countRemainingPegs(grid));
    }


}
