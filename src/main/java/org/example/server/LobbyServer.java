package org.example.server;

import org.jspace.*;
import java.util.*;

public class LobbyServer {
    private static final String LOBBY_URI = "tcp://127.0.0.1:9001/lobby?keep";

    private static SpaceRepository repository;
    private static Space lobbySpace;
    // Keep track of each player's readiness
    private static final Map<String, Boolean> players = new HashMap<>();
    private static boolean gameStarted = false; // Flag to prevent multiple triggers

    public static void main(String[] args) {
        repository = new SpaceRepository();
        lobbySpace = new SequentialSpace();
        repository.add("lobby", lobbySpace);

        try {
            // Start the gate on port 9001
            repository.addGate("tcp://127.0.0.1:9001/?keep");
            System.out.println("LobbyServer started and listening on " + LOBBY_URI);
            System.out.println("Gate successfully opened at 127.0.0.1:9001/?keep");
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
     * Periodically broadcasts ("UPDATE_ALL", Map<String, Boolean>) once every ~200ms.
     * This can be consumed by clients in a single shot to refresh the entire lobby list.
     */
    private static void broadcastUpdates() {
        while (true) {
            try {
                Thread.sleep(200); // broadcast 5 times/sec, adjust as needed

                synchronized (players) {
                    // 1) Remove any old "UPDATE_ALL" tuple to avoid backlog
                    lobbySpace.getAll(
                            new ActualField("UPDATE_ALL"),
                            new FormalField(Object.class)
                    );

                    // 2) Create a snapshot of the current player map
                    //    - If you want stable insertion order, consider using LinkedHashMap in 'players'.
                    //      Or create a new LinkedHashMap here from 'players'.
                    Map<String, Boolean> snapshot = new LinkedHashMap<>(players);

                    // 3) Put a single tuple with the entire map
                    // Key: "UPDATE_ALL", Value: snapshot
                    lobbySpace.put("UPDATE_ALL", snapshot);
                }
            } catch (InterruptedException e) {
                System.err.println("Error broadcasting updates: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if every player in the lobby is ready (and not empty).
     * If so, starts the game after 3 seconds by putting START_GAME tuples:
     * ("START_GAME", "SERVER") + ("START_GAME", eachPlayerName).
     */
    private static void checkGameStart() {
        // Count how many players are ready
        long readyCount = players.values().stream()
                .filter(Boolean::booleanValue)
                .count();

        // If at least 2 players are ready, start the game
        if (!gameStarted && readyCount >= 2) {
            gameStarted = true;
            System.out.println("At least 2 players are ready. Starting the game in 3 seconds!");

            new Thread(() -> {
                try {
                    Thread.sleep(3000);

                    synchronized (players) {
                        // (1) Signal the TankServer
                        lobbySpace.put("START_GAME", "SERVER");

                        // (2) For each *ready* player (or all players, if you prefer)
                        // you can send them their own START_GAME tuple.
                        for (Map.Entry<String, Boolean> entry : players.entrySet()) {
                            if (entry.getValue()) {
                                lobbySpace.put("START_GAME", entry.getKey());
                            }
                        }
                    }
                    System.out.println("START_GAME signal sent to the server and each ready client.");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}

