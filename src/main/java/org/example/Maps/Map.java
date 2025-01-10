package org.example.Maps;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Map extends Application {

    private static final int GRID_SIZE = 15;
    private static final int CELL_SIZE = 40;
    private static final int WALL_THICKNESS = 1;

    // Maze dimensions and structures
    private boolean[][] verticalWalls;
    private boolean[][] horizontalWalls;
    private boolean[][] visited;

    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        root.setPrefSize(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);

        // Initialize maze structures
        verticalWalls = new boolean[GRID_SIZE][GRID_SIZE + 1];
        horizontalWalls = new boolean[GRID_SIZE + 1][GRID_SIZE];
        visited = new boolean[GRID_SIZE][GRID_SIZE];

        // Generate maze
        generateMaze(0, 0);

        // Draw the maze
        drawMaze(root);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Maze Game with Path");
        primaryStage.show();
    }

    private void generateMaze(int x, int y) {
        visited[x][y] = true;


        List<int[]> directions = new ArrayList<>();
        directions.add(new int[]{1, 0}); // Down
        directions.add(new int[]{-1, 0}); // Up
        directions.add(new int[]{0, 1}); // Right
        directions.add(new int[]{0, -1}); // Left

        // Shuffle directions to create a random maze
        Collections.shuffle(directions);

        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];


            if (nx >= 0 && ny >= 0 && nx < GRID_SIZE && ny < GRID_SIZE && !visited[nx][ny]) {

                if (dir[0] == 1) {
                    horizontalWalls[x + 1][y] = true;
                } else if (dir[0] == -1) {
                    horizontalWalls[x][y] = true;
                } else if (dir[1] == 1) {
                    verticalWalls[x][y + 1] = true;
                } else if (dir[1] == -1) {
                    verticalWalls[x][y] = true;
                }

                generateMaze(nx, ny);
            }
        }
    }

    private void drawMaze(Pane root) {
        // Draw horizontal walls
        for (int i = 0; i <= GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (!horizontalWalls[i][j]) {
                    Rectangle wall = new Rectangle(
                            j * CELL_SIZE,
                            i * CELL_SIZE - WALL_THICKNESS / 2.0,
                            CELL_SIZE,
                            WALL_THICKNESS
                    );
                    wall.setFill(Color.BLACK);
                    root.getChildren().add(wall);
                }
            }
        }

        // Draw vertical walls
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j <= GRID_SIZE; j++) {
                if (!verticalWalls[i][j]) {
                    Rectangle wall = new Rectangle(
                            j * CELL_SIZE - WALL_THICKNESS / 2.0,
                            i * CELL_SIZE,
                            WALL_THICKNESS,
                            CELL_SIZE
                    );
                    wall.setFill(Color.BLACK);
                    root.getChildren().add(wall);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}