package org.example.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Map {
    private static final int GRID_SIZE = 15;
    private static final int CELL_SIZE = 40;

    private final List<Wall> walls = new ArrayList<>();
    private final Random random = new Random();

    public Map() {
        initializeWalls();
        ensureTwoWallsRemovedPerCell();
    }

    private void initializeWalls() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                walls.add(new Wall(j * CELL_SIZE, i * CELL_SIZE, (j + 1) * CELL_SIZE, i * CELL_SIZE)); // Top wall
                walls.add(new Wall(j * CELL_SIZE, i * CELL_SIZE, j * CELL_SIZE, (i + 1) * CELL_SIZE)); // Left wall

                if (i == GRID_SIZE - 1) {
                    walls.add(new Wall(j * CELL_SIZE, (i + 1) * CELL_SIZE, (j + 1) * CELL_SIZE, (i + 1) * CELL_SIZE)); // Bottom wall
                }
                if (j == GRID_SIZE - 1) {
                    walls.add(new Wall((j + 1) * CELL_SIZE, i * CELL_SIZE, (j + 1) * CELL_SIZE, (i + 1) * CELL_SIZE)); // Right wall
                }
            }
        }
    }



    private void ensureTwoWallsRemovedPerCell() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                removeRandomWall(j, i);
                removeRandomWall(j, i);
            }
        }
    }

    private void removeRandomWall(int x, int y) {
        List<Wall> cellWalls = new ArrayList<>();

        if (y > 0) {
            cellWalls.add(findWall(x * CELL_SIZE, y * CELL_SIZE, (x + 1) * CELL_SIZE, y * CELL_SIZE));
        }
        if (x < GRID_SIZE - 1) {
            cellWalls.add(findWall((x + 1) * CELL_SIZE, y * CELL_SIZE, (x + 1) * CELL_SIZE, (y + 1) * CELL_SIZE));
        }
        if (y < GRID_SIZE - 1) {
            cellWalls.add(findWall(x * CELL_SIZE, (y + 1) * CELL_SIZE, (x + 1) * CELL_SIZE, (y + 1) * CELL_SIZE));
        }
        if (x > 0) {
            cellWalls.add(findWall(x * CELL_SIZE, y * CELL_SIZE, x * CELL_SIZE, (y + 1) * CELL_SIZE));
        }

        Wall wallToRemove = cellWalls.stream()
                .filter(wall -> wall != null)
                .skip(random.nextInt(cellWalls.size()))
                .findFirst()
                .orElse(null);

        if (wallToRemove != null) {
            walls.remove(wallToRemove);
        }
    }

    private Wall findWall(double startX, double startY, double endX, double endY) {
        return walls.stream()
                .filter(wall ->
                        wall.getStartX() == startX &&
                                wall.getStartY() == startY &&
                                wall.getEndX() == endX &&
                                wall.getEndY() == endY)
                .findFirst()
                .orElse(null);
    }


    public List<Wall> getWalls() {
        return walls;
    }
}
