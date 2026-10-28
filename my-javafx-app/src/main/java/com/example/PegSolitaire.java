package com.example;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class PegSolitaire extends Application {

    enum GameOutcome {
        WON,
        LOST
    }

    record GameOverResult(
        GameOutcome outcome,
        int remainingPegs,
        String title,
        String message,
        List<String> actions
    ) {}

    static final class GameState {
        private int[][] grid;
        private int score;
        private int secondsElapsed;

        void startNewGame(BoardType boardType) {
            grid = boardType.createInitialGrid();
            score = 0;
            secondsElapsed = 0;
        }

        boolean restartIfConfirmed(BoardType boardType, BooleanSupplier confirmation) {
            if (!confirmation.getAsBoolean()) {
                return false;
            }

            startNewGame(boardType);
            return true;
        }

        int[][] getGrid() {
            return grid;
        }

        int getScore() {
            return score;
        }

        int getSecondsElapsed() {
            return secondsElapsed;
        }

        void addScore(int points) {
            score += points;
        }

        void incrementSecondsElapsed() {
            secondsElapsed++;
        }       
        
    }

    public enum BoardType {
    ENGLISH_33("English (33 Holes)", new int[][]{
        {0, 0, 1, 1, 1, 0, 0},
        {0, 0, 1, 1, 1, 0, 0},
        {1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 2, 1, 1, 1}, // Center empty
        {1, 1, 1, 1, 1, 1, 1},
        {0, 0, 1, 1, 1, 0, 0},
        {0, 0, 1, 1, 1, 0, 0}
    }),
    DIAMOND_41("Diamond (41 Holes)", new int[][]{
        {0, 0, 0, 0, 1, 0, 0, 0, 0},
        {0, 0, 0, 1, 1, 1, 0, 0, 0},
        {0, 0, 1, 1, 1, 1, 1, 0, 0},
        {0, 1, 1, 1, 1, 1, 1, 1, 0},
        {1, 1, 1, 1, 2, 1, 1, 1, 1}, // Center empty
        {0, 1, 1, 1, 1, 1, 1, 1, 0},
        {0, 0, 1, 1, 1, 1, 1, 0, 0},
        {0, 0, 0, 1, 1, 1, 0, 0, 0},
        {0, 0, 0, 0, 1, 0, 0, 0, 0}
    }),

    HEXAGON_21("Hexagon (21 Holes)", new int[][]{
        {0, 0, 1, 1, 1, 0, 0},
        {0, 1, 1, 1, 1, 1, 0},
        {0, 1, 1, 2, 1, 1, 0}, // Center empty
        {0, 1, 1, 1, 1, 1, 0},
        {0, 0, 1, 1, 1, 0, 0}
    });

    private final String displayName;
    private final int[][] defaultGrid;

    BoardType(String displayName, int[][] defaultGrid) {
        this.displayName = displayName;
        this.defaultGrid = defaultGrid;
    }

    public String getDisplayName() { return displayName; }

    public int[][] createInitialGrid() {
        // Deep copy array so mutations don't overwrite original template
        int rows = defaultGrid.length;
        int cols = defaultGrid[0].length;
        int[][] copy = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(defaultGrid[r], 0, copy[r], 0, cols);
        }
        return copy;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
    private final GameState gameState = new GameState();
    private GridPane boardContainer;
    private ComboBox<BoardType> boardSelector;

    // Game Metrics (AC 3.2)
    private Label scoreLabel;
    private Label timerLabel;
    private Timeline gameTimer;
    private boolean gameInProgress = false;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        boardSelector = new ComboBox<>();
        boardSelector.getItems().setAll(BoardType.values());
        boardSelector.setValue(BoardType.ENGLISH_33);

        boardSelector.setOnAction(e -> handleBoardSelectionChange());

        Button restartButton = new Button("Restart");
        restartButton.setOnAction(e -> handleRestartRequest());

        scoreLabel = new Label("Score: 0");
        timerLabel = new Label("Time: 00:00");

        String metricStyle = "-fx-font-weight: bold; -fx-font-size: 13px;";
        scoreLabel.setStyle(metricStyle);
        timerLabel.setStyle(metricStyle);

        HBox topBar = new HBox(12, 
            new Label("Board Size and Type:"), 
            boardSelector, 
            restartButton,
            scoreLabel,
            timerLabel
        );
        topBar.setAlignment(Pos.CENTER);
        topBar.setStyle("-fx-padding: 15px; -fx-background-color: #e4baba;");
        root.setTop(topBar);

        boardContainer = new GridPane();
        boardContainer.setAlignment(Pos.CENTER);
        boardContainer.setHgap(8);
        boardContainer.setVgap(8);
        boardContainer.setStyle("-fx-padding: 20px;");
        root.setCenter(boardContainer);

        setupTimer();

        initiateNewGame();

        Scene scene = new Scene(root, 550, 600);
        primaryStage.setTitle("Peg Solitaire");
        primaryStage.setScene(scene);
        primaryStage.show();

    }


    private void handleBoardSelectionChange() {
        if (gameInProgress) {
            if (confirmRestart("Change Board Type", "Are you sure you want to change the board?")) {
                initiateNewGame();
            } else {
                // Revert dropdown back to currently loaded board
                boardSelector.setValue(getCurrentBoardType());
            }
        } else {
            initiateNewGame();
        }
    }

    private void handleRestartRequest() {
        if (gameInProgress) {
            if (gameState.restartIfConfirmed(
                    boardSelector.getValue(),
                    () -> confirmRestart("Restart Game", "Are you sure you want to restart?"))) {
                selectedRow = -1;
                selectedCol = -1;
                resetMetrics();
                renderBoard();
            }
        } else {
            initiateNewGame();
        }
    }

    private boolean confirmRestart(String title, String header) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText("Your current progress, score, and timer will be reset.");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * AC 3.1 & 3.2: Resets metrics, loads fresh board array, and renders UI.
     */
    private void initiateNewGame() {
        BoardType selectedType = boardSelector.getValue();
        if (selectedType == null) return;

        selectedRow = -1;
        selectedCol = -1;

        // AC 3.1: Reset matrix to starting positions
        gameState.startNewGame(selectedType);

        // AC 3.2: Reset metrics
        resetMetrics();

        // Render starting board
        renderBoard();

        gameInProgress = true;
    }

    /**
     * AC 3.2: Sets timer and score to 0.
     */
    private void resetMetrics() {
        scoreLabel.setText("Score: " + gameState.getScore());
        timerLabel.setText(String.format("Time: %02d:%02d",
            gameState.getSecondsElapsed() / 60, gameState.getSecondsElapsed() % 60));

        if (gameTimer != null) {
            gameTimer.playFromStart();
        }
    }

    private void setupTimer() {
        gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            gameState.incrementSecondsElapsed();
            int mins = gameState.getSecondsElapsed() / 60;
            int secs = gameState.getSecondsElapsed() % 60;
            timerLabel.setText(String.format("Time: %02d:%02d", mins, secs));
        }));
        gameTimer.setCycleCount(Timeline.INDEFINITE);
    }

    private BoardType getCurrentBoardType() {
        for (BoardType type : BoardType.values()) {
            if (type.getDisplayName().equals(boardSelector.getValue().getDisplayName())) {
                return type;
            }
        }
        return BoardType.ENGLISH_33;
    }

    private void renderBoard() {
        boardContainer.getChildren().clear();

        int[][] currentGrid = gameState.getGrid();
        int rows = currentGrid.length;
        int cols = currentGrid[0].length;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cellState = currentGrid[r][c];

                if (cellState == 0) {
                    continue;
                }

                StackPane cellNode = createCellNode(r, c, cellState);
                boardContainer.add(cellNode, c, r);
            }
        }
    }
        private int selectedRow = -1;
        private int selectedCol = -1;

    private StackPane createCellNode(int row, int col, int cellState) {
        StackPane cell = new StackPane();

        Circle hole = new Circle(18);
        hole.setFill(Color.rgb(60, 40, 20));
        hole.setStroke(Color.BLACK);
        cell.getChildren().add(hole);

        if (cellState == 1) {
            Circle peg = new Circle(13);

            if (row == selectedRow && col == selectedCol){
                peg.setFill(Color.GOLD);
                peg.setStroke(Color.ORANGE);
            } else {
            peg.setFill(Color.CRIMSON);
            peg.setStroke(Color.MAROON);
            }

            cell.getChildren().add(peg);
        }
        cell.setOnMouseClicked(e -> handleCellClick(row, col));
        return cell;
    }

    private void handleCellClick(int row, int col) {
        int[][] currentGrid = gameState.getGrid();
        int cellState = currentGrid[row][col];

        if (cellState == 1) {
            if (selectedRow == row && selectedCol == col){
                selectedRow = -1;
                selectedCol = -1;
            } else {
                selectedRow = row;
                selectedCol = col;
            }
            renderBoard();
            return;
        }

        if (cellState == 2 && selectedRow != -1 && selectedCol != -1){
            if (isValidMove(currentGrid, selectedRow, selectedCol, row, col)) {
                executeMove(selectedRow, selectedCol, row, col);
            } else{
                System.out.println("Invalid move");
            }
        }
    }

    static boolean isValidMove(int[][] grid, int fromR, int fromC, int toR, int toC) {
        if (fromR < 0 || fromR >= grid.length ||
            fromC < 0 || fromC >= grid[fromR].length ||
            toR < 0 || toR >= grid.length ||
            toC < 0 || toC >= grid[toR].length) {
            return false;
        }

        // The source must have a peg and the destination must be an empty hole.
        if (grid[fromR][fromC] != 1) return false;
        if (grid[toR][toC] != 2) return false;

        int rowDiff = Math.abs(fromR - toR);
        int colDiff = Math.abs(fromC - toC);

        // 2. Must move strictly along 1 axis by exactly 2 spaces
        boolean isOrthogonal2Spaces = (rowDiff == 2 && colDiff == 0) || (rowDiff == 0 && colDiff == 2);
        if (!isOrthogonal2Spaces) return false;

        // 3. Find the middle cell being jumped over
        int midR = (fromR + toR) / 2;
        int midC = (fromC + toC) / 2;

        // 4. Middle cell must contain a peg (State 1)
        return grid[midR][midC] == 1;
    }

    static boolean applyMove(int[][] grid, int fromR, int fromC, int toR, int toC) {
        if (!isValidMove(grid, fromR, fromC, toR, toC)) {
            return false;
        }

        int midR = (fromR + toR) / 2;
        int midC = (fromC + toC) / 2;
        grid[fromR][fromC] = 2;
        grid[midR][midC] = 2;
        grid[toR][toC] = 1;
        return true;
    }

    private void executeMove(int fromR, int fromC, int toR, int toC) {
        int[][] currentGrid = gameState.getGrid();
        if (!applyMove(currentGrid, fromR, fromC, toR, toC)) {
            return;
        }

        // Clear Selection State
        selectedRow = -1;
        selectedCol = -1;

        // Update Score (+10 per successful move)
        gameState.addScore(10);
        scoreLabel.setText("Score: " + gameState.getScore());

        // Re-render Board UI
        renderBoard();

        checkGameOver();
}

    static Optional<GameOverResult> getGameOverResult(int[][] grid, int score, String time) {
        if (hasAnyValidMoves(grid)) {
            return Optional.empty();
        }

        int remainingPegs = countRemainingPegs(grid);
        if (remainingPegs == 1) {
            return Optional.of(new GameOverResult(
                GameOutcome.WON,
                remainingPegs,
                "YOU WINNNNN!",
                String.format(
                    "1 peg remaining!\nFinal Score: %d | Time: %s",
                    score,
                    time
                ),
                List.of("Restart", "Exit")
            ));
        }

        return Optional.of(new GameOverResult(
            GameOutcome.LOST,
            remainingPegs,
            "Game Over",
            String.format(
                "Game Over :O You have %d pegs remaining. \nFinal Score: %d | Time: %s",
                remainingPegs,
                score,
                time
            ),
            List.of("Restart", "Exit")
        ));
    }

    private void checkGameOver() {
        Optional<GameOverResult> result = getGameOverResult(
            gameState.getGrid(),
            gameState.getScore(),
            timerLabel.getText().replace("Time: ", "")
        );
        if (result.isEmpty()) {
            return;
        }

        GameOverResult gameOver = result.get();
        if (gameTimer != null) {
            gameTimer.stop();
        }
        gameInProgress = false;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        ButtonType restartBtn = new ButtonType(
            gameOver.actions().get(0),
            ButtonBar.ButtonData.OK_DONE
        );
        ButtonType exitBtn = new ButtonType(
            gameOver.actions().get(1),
            ButtonBar.ButtonData.CANCEL_CLOSE
        );
        alert.getButtonTypes().setAll(restartBtn, exitBtn);
        alert.setTitle(gameOver.title());
        alert.setContentText(gameOver.message());

        alert.showAndWait().ifPresent(response -> {
            if (response == restartBtn) {
                initiateNewGame();
            } else if (response == exitBtn) {
                Platform.exit();
            }
        });
    }

    static int countRemainingPegs(int[][] grid) {
        int pegs = 0;
        for (int[] row : grid) {
            for (int cell : row) {
                if (cell == 1) pegs++;
            }
        }
        return pegs;
    }
    static boolean hasAnyValidMoves(int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;

        // Check every cell on the board
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 1) { // Found a peg
                    // Check all 4 orthogonal directions: Up, Down, Left, Right
                    if (isValidMove(grid, r, c, r - 2, c) || // Up
                        isValidMove(grid, r, c, r + 2, c) || // Down
                        isValidMove(grid, r, c, r, c - 2) || // Left
                        isValidMove(grid, r, c, r, c + 2)) { // Right
                        return true; // At least one valid move exists
                    }
                }
            }
        }
        return false; // No valid moves remaining anywhere
    }

public static void main(String[] args) {
        launch(args);
    }
}