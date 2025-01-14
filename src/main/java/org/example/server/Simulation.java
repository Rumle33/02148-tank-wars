package org.example.server;

import org.example.Maps.Wall;
import org.jspace.Space;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Simulation {
    private final Space gameSpace;
    private final Map<String, Tank> playerTanks = new HashMap<>();
    private List<Wall> mapWalls;
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

    public void run() {
        new Thread(this::handleMapRequests).start();

        try {
            while (true) {
                processPlayerActions();

                dynamicBuffer.clear();

                for (GameObject object : dynamicObjects) {
                    if (object.update(this, 1.0f / 60.0f)) { // Update objects at 60 FPS
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
