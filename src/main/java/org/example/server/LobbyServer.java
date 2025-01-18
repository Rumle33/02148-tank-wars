package org.example.server;

import org.jspace.*;
import java.util.*;

public class LobbyServer {
    private static final String LOBBY_URI = "tcp://127.0.0.1:9001/lobby?keep";

    private static SpaceRepository repository;
    private static Space lobbySpace;
    private static final Map<String, Boolean> players = new HashMap<>();
    private static boolean gameStarted = false; // Flag to prevent multiple triggers

    // Speed up broadcast so clients see changes quickly
    private static final int BROADCAST_DELAY_MS = 300;

    public static void main(String[] args) {
        repository = new SpaceRepository();
        lobbySpace = new SequentialSpace();
        repository.add("lobby", lobbySpace);

        try {
            // Open the TCP gate on port 9001
            repository.addGate("tcp://127.0.0.1:9001/?keep");
            System.out.println("LobbyServer started on " + LOBBY_URI);

            // 1) Clear out any old data in case of leftover from previous runs
            lobbySpace.getAll(new ActualField("UPDATE"), new FormalField(String.class), new FormalField(Boolean.class));
            lobbySpace.getAll(new ActualField("START_GAME"), new FormalField(String.class));
            // Remove any old chat messages
            lobbySpace.getAll(new ActualField("CHAT_MSG"), new FormalField(String.class), new FormalField(String.class));
            System.out.println("Cleaned up old tuples in the lobby space.");

        } catch (Exception e) {
            System.err.println("Error opening gate: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Threads to handle actions and to broadcast "UPDATE"
        new Thread(LobbyServer::handleLobby).start();
        new Thread(LobbyServer::broadcastUpdates).start();
    }

    /**
     * Continuously wait for player actions: JOIN, LEAVE, READY, NOT_READY.
     * (Does NOT handle CHAT_MSG, so those remain in the space.)
     */
    private static void handleLobby() {
        try {
            while (true) {
                System.out.println("Waiting for player actions...");
                // Wait for a tuple of shape (String, String)
                Object[] tuple = lobbySpace.get(new FormalField(String.class), new FormalField(String.class));
                String playerName = (String) tuple[0];
                String action = (String) tuple[1];
                System.out.println("Received action: " + action + " from: " + playerName);

                synchronized (players) {
                    switch (action) {
                        case "JOIN":
                            players.put(playerName, false);
                            System.out.println(playerName + " joined the lobby.");
                            break;

                        case "LEAVE":
                            players.remove(playerName);
                            System.out.println(playerName + " left the lobby.");
                            // Remove that player's "UPDATE" tuple so they vanish from clients
                            lobbySpace.getAll(
                                    new ActualField("UPDATE"),
                                    new ActualField(playerName),
                                    new FormalField(Boolean.class)
                            );
                            // Optionally, remove any chat messages from them if you want:
                            // lobbySpace.getAll(new ActualField("CHAT_MSG"), new ActualField(playerName), new FormalField(String.class));
                            break;

                        case "READY":
                            players.put(playerName, true);
                            System.out.println(playerName + " is ready.");
                            break;

                        case "NOT_READY":
                            players.put(playerName, false);
                            System.out.println(playerName + " is not ready.");
                            break;

                        default:
                            System.err.println("Unknown action: " + action);
                            break;
                    }

                    // After each action, see if we should start the game
                    checkGameStart();
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Error in handleLobby: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Periodically updates ("UPDATE", playerName, isReady) for each player.
     */
    private static void broadcastUpdates() {
        while (true) {
            try {
                Thread.sleep(BROADCAST_DELAY_MS);

                synchronized (players) {
                    // For each known player, remove old "UPDATE" then insert fresh
                    for (String name : players.keySet()) {
                        lobbySpace.getp(new ActualField("UPDATE"), new ActualField(name), new FormalField(Boolean.class));
                        Boolean isReady = players.get(name);
                        lobbySpace.put("UPDATE", name, isReady);
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("Error broadcasting updates: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * If all players are ready, start the game after 3 seconds and remove chat messages.
     */
    private static void checkGameStart() {
        if (!gameStarted && !players.isEmpty() && players.values().stream().allMatch(ready -> ready)) {
            gameStarted = true;
            System.out.println("All players ready! Game will start in 3 seconds...");

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    synchronized (players) {
                        // Remove all chat messages before the game starts (optional)
                        lobbySpace.getAll(
                                new ActualField("CHAT_MSG"),
                                new FormalField(String.class),
                                new FormalField(String.class)
                        );
                        System.out.println("Removed all chat messages from the lobby.");

                        // Send START_GAME to server + each client
                        lobbySpace.put("START_GAME", "SERVER");
                        for (String pName : players.keySet()) {
                            lobbySpace.put("START_GAME", pName);
                        }
                        System.out.println("START_GAME signal sent.");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
