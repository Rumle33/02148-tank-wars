package org.example.server;

import org.jspace.Space;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Simulation {
    private static final int UPS = 50;
    private final Space gameSpace;
    private final Map<String, Tank> playerTanks = new HashMap<>();
    private final List<Projectile> projectiles = new ArrayList<>();

    public Simulation(Space gameSpace, List<String> players) {
        this.gameSpace = gameSpace;
        initializeTanks(players);
    }

    private void initializeTanks(List<String> players) {
        int i = 0;
        for (String player : players) {
            Tank tank = new Tank();
            tank.x = 200 + (i * 400); // Unique starting positions
            tank.y = 300;
            playerTanks.put(player, tank);
            i++;
            System.out.println("Initialized tank for " + player + " at x=" + tank.x + ", y=" + tank.y);
        }
    }

    public void run() {
        long lastTime = System.currentTimeMillis();

        while (true) {
            long currentTime = System.currentTimeMillis();
            float delta = (currentTime - lastTime) / 1000.0f;

            processPlayerActions();
            updateTanks(delta);
            updateProjectiles(delta);
            broadcastGameState();

            lastTime = currentTime;

            try {
                Thread.sleep(1000 / UPS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
                            // Create a new projectile at the tank's current position and direction
                            Projectile projectile = new Projectile(tank.x, tank.y, tank.rotation);
                            projectiles.add(projectile);
                            System.out.println("Projectile fired by " + playerName + " at x=" + tank.x + ", y=" + tank.y);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void updateTanks(float delta) {
        for (Tank tank : playerTanks.values()) {
            tank.update(delta);
        }
    }

    private void updateProjectiles(float delta) {
        for (Iterator<Projectile> iterator = projectiles.iterator(); iterator.hasNext(); ) {
            Projectile projectile = iterator.next();
            projectile.update(delta);
            if (!projectile.isAlive()) {
                iterator.remove();
            }
        }
    }

    private void broadcastGameState() {
        try {
            StringBuilder state = new StringBuilder();
            for (Map.Entry<String, Tank> entry : playerTanks.entrySet()) {
                String playerName = entry.getKey();
                Tank tank = entry.getValue();
                state.append(playerName).append(" ")
                        .append(tank.x).append(" ")
                        .append(tank.y).append(" ")
                        .append(tank.rotation).append("\n");
            }
            for (Projectile projectile : projectiles) {
                state.append("Projectile ")
                        .append(projectile.x).append(" ")
                        .append(projectile.y).append(" ")
                        .append(projectile.rotation).append("\n");
            }
            gameSpace.put("STATE", state.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
