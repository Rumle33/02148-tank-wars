package org.example.server;

import org.example.Maps.Wall;
import org.jspace.Space;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Simulation {
    private final Space gameSpace;
    private final Map<String, Tank> playerTanks = new HashMap<>();
    private final List<Wall> mapWalls;
    private List<GameObject> dynamicObjects = new ArrayList<>();
    private List<GameObject> dynamicBuffer = new ArrayList<>();

    public Simulation(Space gameSpace, List<String> players) {
        this.gameSpace = gameSpace;
        this.mapWalls = new org.example.Maps.Map().getWalls();
        initializeTanks(players);
    }

    private void initializeTanks(List<String> players) {
        int i = 0;
        for (String player : players) {
            Tank tank = new Tank(player, 0);
            tank.x = 200 + (i * 400); // Unique starting positions
            tank.y = 300;
            playerTanks.put(player, tank);
            this.dynamicObjects.add(tank);
            i++;
        }
    }

    public void handleMapRequests() {
        try {
            while (true) {
                Object[] request = gameSpace.get(new org.jspace.ActualField("REQUEST_MAP"), new org.jspace.FormalField(String.class));
                String requestingPlayer = (String) request[1];
                System.out.println("Sending map to: " + requestingPlayer);
                sendMap();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendMap() {
        try {
            StringBuilder mapState = new StringBuilder();
            for (Wall wall : mapWalls) {
                mapState.append(wall.getStartX()).append(" ")
                        .append(wall.getStartY()).append(" ")
                        .append(wall.getEndX()).append(" ")
                        .append(wall.getEndY()).append("\n");
            }
            gameSpace.put("MAP", mapState.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void processPlayerActions() {
        try {
            Object[] action;
            while ((action = gameSpace.getp(
                    new org.jspace.ActualField("ACTION"),
                    new org.jspace.FormalField(String.class),
                    new org.jspace.FormalField(String.class),
                    new org.jspace.FormalField(Float.class)
            )) != null) {
                String playerName = (String) action[1];
                String command = (String) action[2];
                float value = (Float) action[3];

                Tank tank = playerTanks.get(playerName);
                if (tank != null) {
                    switch (command) {
                        case "MOVE" -> tank.acceleration = value * tank.maxVelocity;
                        case "ROTATE" -> tank.angularAcceleration = value * tank.maxAngularVelocity;
                        case "SHOOT" -> {
                            Projectile projectile = new Projectile(tank.x, tank.y, tank.rotation);
                            dynamicObjects.add(projectile);
                            System.out.println("Projectile created at (" + tank.x + ", " + tank.y + ")");
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean handleTankWallCollision(Tank tank, float dx, float dy) {
        for (Wall wall : mapWalls) {
            if (AABBCollision.test(tank, wall)) {
                // Stop velocity and undo movement
                tank.velocity = 0;
                tank.x -= dx; // Undo horizontal movement
                tank.y -= dy; // Undo vertical movement

                // Log for debugging
                System.out.println("Collision detected! Tank stopped at: (" + tank.x + ", " + tank.y + ")");
                return true; // Collision occurred
            }
        }
        return false; // No collision
    }



    private void handleProjectileWallCollision(Projectile projectile) {
        for (Wall wall : mapWalls) {
            if (AABBCollision.test(projectile, wall)) {
                // Detect if the collision is near a corner
                boolean nearCornerX = Math.abs(projectile.x - wall.getStartX()) < 1.0f || Math.abs(projectile.x - wall.getEndX()) < 1.0f;
                boolean nearCornerY = Math.abs(projectile.y - wall.getStartY()) < 1.0f || Math.abs(projectile.y - wall.getEndY()) < 1.0f;

                if (nearCornerX && nearCornerY) {
                    // If near a corner, reverse both X and Y directions
                    projectile.rotation = (float) (projectile.rotation + Math.PI);
                } else {
                    // Handle standard horizontal/vertical collisions
                    boolean isHorizontal = Math.abs(wall.getEndY() - wall.getStartY()) < Math.abs(wall.getEndX() - wall.getStartX());

                    if (isHorizontal) {
                        projectile.rotation = (float) (-projectile.rotation);
                    } else {
                        projectile.rotation = (float) (Math.PI - projectile.rotation);
                    }
                }

                // Adjust the projectile's position slightly to avoid re-collision
                float adjustment = 2.0f;
                projectile.x += (float) (Math.cos(projectile.rotation) * adjustment);
                projectile.y += (float) (Math.sin(projectile.rotation) * adjustment);

                return; // Exit after handling the collision
            }
        }
    }




    public void run() {
        new Thread(this::handleMapRequests).start();

        try {
            while (true) {
                processPlayerActions();

                dynamicBuffer.clear();

                for (GameObject object : dynamicObjects) {
                    if (object.update(this, 1.0f / 60.0f)) {
                        if (object instanceof Tank tank) {
                            // Calculate dx and dy based on the tank's movement
                            float dx = (float) (tank.velocity * Math.cos(tank.rotation) * (1.0f / 60.0f));
                            float dy = (float) (tank.velocity * Math.sin(tank.rotation) * (1.0f / 60.0f));

                            // Pass the dx and dy values along with the tank to handleTankWallCollision
                            handleTankWallCollision(tank, dx, dy);
                        } else if (object instanceof Projectile) {
                            handleProjectileWallCollision((Projectile) object);
                        }
                        dynamicBuffer.add(object);
                    }
                }


                // Swap buffers
                List<GameObject> temp = dynamicBuffer;
                dynamicBuffer = dynamicObjects;
                dynamicObjects = temp;

                broadcastGameState();
                Thread.sleep(1000 / 60); // Maintain 60 FPS
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void broadcastGameState() {
        try {
            StringBuilder state = new StringBuilder();
            for (GameObject object : dynamicObjects) {
                object.serialize(state);
            }
            gameSpace.put("STATE", state.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
