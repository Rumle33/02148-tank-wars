package org.example.server;

import org.jspace.*;
import java.util.*;

public class LobbyServer {
    private static final String LOBBY_URI = "tcp://127.0.0.1:9001/lobby?keep";

    private static SpaceRepository repository;
    private static Space lobbySpace;
    private static final Map<String, Boolean> players = new HashMap<>();
    private static boolean gameStarted = false; // Flag to prevent multiple triggers

    public static void main(String[] args) {
        repository = new SpaceRepository();
        lobbySpace = new SequentialSpace();
        repository.add("lobby", lobbySpace);

        try {
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
     * Periodically broadcasts ("UPDATE", playerName, isReady) tuples for each player.
     * This can be consumed by clients to refresh the lobby list.
     * <p>
     * Note: This example removes old "UPDATE" tuples to avoid a huge backlog in the space.
     */
    private static void broadcastUpdates() {
        while (true) {
            try {
                Thread.sleep(500); // Adjust frequency as needed (500ms, for example)

                synchronized (players) {
                    // 1) Remove old "UPDATE" tuples to prevent build-up (optional but recommended)
                    for (String name : players.keySet()) {
                        // Remove all matching UPDATE tuples for this name
                        lobbySpace.getAll(
                                new ActualField("UPDATE"),
                                new ActualField(name),
                                new FormalField(Boolean.class)
                        );
                    }

                    // 2) Insert the latest status
                    for (Map.Entry<String, Boolean> entry : players.entrySet()) {
                        String playerName = entry.getKey();
                        Boolean isReady = entry.getValue();
                        lobbySpace.put("UPDATE", playerName, isReady);
                    }
                }
                // System.out.println("Broadcasted updated player list to all clients.");
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
