package org.example.Maps;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Map extends Application {

    private static final int GRID_SIZE = 15;  // Size of the grid
    private static final int CELL_SIZE = 40; // Size of each cell

    private final List<Wall> walls = new ArrayList<>(); // To store all walls
    private final Random random = new Random();

    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        root.setPrefSize(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);

        // Initialize walls
        initializeWalls();

        // Remove at least 2 walls for each cell
        ensureTwoWallsRemovedPerCell();

        // Draw the map
        drawWalls(root);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Tank Trouble Map Generator");
        primaryStage.show();
    }

    /**
     * Initializes all walls for the grid.
     */
    private void initializeWalls() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                // Add walls for the current cell
                walls.add(new Wall(j * CELL_SIZE, i * CELL_SIZE, (j + 1) * CELL_SIZE, i * CELL_SIZE)); // Top wall
                walls.add(new Wall(j * CELL_SIZE, i * CELL_SIZE, j * CELL_SIZE, (i + 1) * CELL_SIZE)); // Left wall

                // Add boundary walls
                if (i == GRID_SIZE - 1) {
                    walls.add(new Wall(j * CELL_SIZE, (i + 1) * CELL_SIZE, (j + 1) * CELL_SIZE, (i + 1) * CELL_SIZE)); // Bottom wall
                }
                if (j == GRID_SIZE - 1) {
                    walls.add(new Wall((j + 1) * CELL_SIZE, i * CELL_SIZE, (j + 1) * CELL_SIZE, (i + 1) * CELL_SIZE)); // Right wall
                }
            }
        }
    }

    /**
     * Ensures at least 2 walls are removed for each cell.
     */
    private void ensureTwoWallsRemovedPerCell() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                // Remove two walls for the current cell
                removeRandomWall(j, i);
                removeRandomWall(j, i);
            }
        }
    }

    /**
     * Removes a random wall for a given cell (x, y).
     */
    private void removeRandomWall(int x, int y) {
        // List of potential walls to remove
        List<Wall> cellWalls = new ArrayList<>();

        // Add the walls of the current cell if they exist
        if (y > 0) {
            cellWalls.add(findWall(x * CELL_SIZE, y * CELL_SIZE, (x + 1) * CELL_SIZE, y * CELL_SIZE)); // Top wall
        }
        if (x < GRID_SIZE - 1) {
            cellWalls.add(findWall((x + 1) * CELL_SIZE, y * CELL_SIZE, (x + 1) * CELL_SIZE, (y + 1) * CELL_SIZE)); // Right wall
        }
        if (y < GRID_SIZE - 1) {
            cellWalls.add(findWall(x * CELL_SIZE, (y + 1) * CELL_SIZE, (x + 1) * CELL_SIZE, (y + 1) * CELL_SIZE)); // Bottom wall
        }
        if (x > 0) {
            cellWalls.add(findWall(x * CELL_SIZE, y * CELL_SIZE, x * CELL_SIZE, (y + 1) * CELL_SIZE)); // Left wall
        }

        // Randomly remove one wall from the list
        Wall wallToRemove = cellWalls.stream()
                .filter(wall -> wall != null) // Ensure the wall exists
                .skip(random.nextInt(cellWalls.size())) // Randomly pick a wall
                .findFirst()
                .orElse(null);

        if (wallToRemove != null) {
            walls.remove(wallToRemove);
        }
    }

    /**
     * Finds a wall matching the given coordinates.
     */
    private Wall findWall(double startX, double startY, double endX, double endY) {
        return walls.stream()
                .filter(wall -> wall.matches(startX, startY, endX, endY))
                .findFirst()
                .orElse(null);
    }

    /**
     * Draws all the remaining walls on the screen.
     */
    private void drawWalls(Pane root) {
        for (Wall wall : walls) {
            Line line = new Line(wall.startX, wall.startY, wall.endX, wall.endY);
            line.setStroke(Color.BLACK);
            line.setStrokeWidth(2);
            root.getChildren().add(line);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}