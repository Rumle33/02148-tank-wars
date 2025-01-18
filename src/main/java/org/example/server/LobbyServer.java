package org.example.server;

import org.jspace.*;
import java.util.*;

public class LobbyServer {
    private static final String LOBBY_URI = "tcp://127.0.0.1:9001/lobby?keep";

    private static SpaceRepository repository;
    private static Space lobbySpace;
    private static final Map<String, Boolean> players = new HashMap<>();
    private static boolean gameStarted = false; // Flag to prevent multiple triggers

    // Adjust the broadcast frequency (milliseconds)
    private static final int BROADCAST_DELAY_MS = 200; // e.g. faster than 500ms

    public static void main(String[] args) {
        repository = new SpaceRepository();
        lobbySpace = new SequentialSpace();
        repository.add("lobby", lobbySpace);

        try {
            // 1) Open the gate
            repository.addGate("tcp://127.0.0.1:9001/?keep");
            System.out.println("LobbyServer started and listening on " + LOBBY_URI);
            System.out.println("Gate successfully opened at 127.0.0.1:9001/?keep");

            // 2) On startup, clear out any old stale "UPDATE" or "START_GAME" tuples
            //    so we start with a completely clean space.
            lobbySpace.getAll(
                    new ActualField("UPDATE"),
                    new FormalField(String.class),
                    new FormalField(Boolean.class)
            );
            lobbySpace.getAll(
                    new ActualField("START_GAME"),
                    new FormalField(String.class)
            );
            System.out.println("Old UPDATE/START_GAME tuples cleared.");
        } catch (Exception e) {
            System.err.println("Error opening gate: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Threads to handle incoming JOIN/LEAVE/READY/NOT_READY and to broadcast updates
        new Thread(LobbyServer::handleLobby).start();
        new Thread(LobbyServer::broadcastUpdates).start();
    }

    /**
     * Continuously wait for player actions: JOIN, LEAVE, READY, NOT_READY.
     */
    private static void handleLobby() {
        try {
            while (true) {
                System.out.println("Waiting for player actions...");
                // Blocks until we get a tuple of shape (String, String)
                Object[] tuple = lobbySpace.get(new FormalField(String.class), new FormalField(String.class));
                String playerName = (String) tuple[0];
                String action = (String) tuple[1];
                System.out.println("Received action: " + action + " from player: " + playerName);

                synchronized (players) {
                    switch (action) {
                        case "JOIN":
                            players.put(playerName, false);
                            System.out.println(playerName + " joined the lobby.");
                            break;

                        case "LEAVE":
                            players.remove(playerName);
                            System.out.println(playerName + " left the lobby.");
                            // Remove any leftover UPDATE tuple so other clients don't see them
                            try {
                                lobbySpace.getAll(
                                        new ActualField("UPDATE"),
                                        new ActualField(playerName),
                                        new FormalField(Boolean.class)
                                );
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
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

                    // After each action, check if everyone is ready
                    checkGameStart();
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Error in handleLobby: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Periodically ensures exactly one ("UPDATE", playerName, isReady) tuple
     * is in the space for each CURRENT player. Removed players won't get new tuples.
     */
    private static void broadcastUpdates() {
        while (true) {
            try {
                Thread.sleep(BROADCAST_DELAY_MS);

                synchronized (players) {
                    // For each known player, remove old "UPDATE" for that player, then insert fresh status
                    for (String name : players.keySet()) {
                        lobbySpace.getp(
                                new ActualField("UPDATE"),
                                new ActualField(name),
                                new FormalField(Boolean.class)
                        );
                        // Insert the latest status
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
     * Checks if every player in the lobby is ready (and not empty).
     * If so, starts the game after 3 seconds by putting START_GAME tuples for each player.
     */
    private static void checkGameStart() {
        if (!gameStarted && !players.isEmpty()
                && players.values().stream().allMatch(ready -> ready)) {

            gameStarted = true;
            System.out.println("All players are ready. Starting the game in 3 seconds!");

            new Thread(() -> {
                try {
                    Thread.sleep(3000);

                    synchronized (players) {
                        // 1) Put one tuple for the server:
                        lobbySpace.put("START_GAME", "SERVER");
                        // 2) Put one tuple for each client:
                        for (String pName : players.keySet()) {
                            lobbySpace.put("START_GAME", pName);
                        }
                    }
                    System.out.println("START_GAME signal sent to the server + each client.");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
