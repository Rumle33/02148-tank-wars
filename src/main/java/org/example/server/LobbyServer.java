package org.example.server;

import org.jspace.*;
import java.util.*;

public class LobbyServer {
    private static final String LOBBY_URI = "tcp://127.0.0.1:9001/lobby?keep";

    private static SpaceRepository repository;
    private static Space lobbySpace;
    private static final Map<String, Boolean> players = new HashMap<>();
    private static boolean gameStarted = false; // Flag to track if the game has started

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

        new Thread(LobbyServer::handleLobby).start();
    }

    private static void handleLobby() {
        try {
            while (true) {
                System.out.println("Waiting for player actions...");
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
                    updatePlayerList();
                    checkGameStart();
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Error in handleLobby: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void updatePlayerList() {
        try {
            for (String player : players.keySet()) {
                boolean isReady = players.get(player);
                lobbySpace.put("UPDATE", player, isReady);
            }
            System.out.println("Broadcasted updated player list to all clients.");
        } catch (InterruptedException e) {
            System.err.println("Error updating player list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkGameStart() {
        if (!gameStarted && players.values().stream().allMatch(ready -> ready)) {
            gameStarted = true; // Set the flag to prevent duplicate triggers
            System.out.println("All players are ready. Starting the game in 3 seconds!");

            // Start the game after a 3-second delay
            new Thread(() -> {
                try {
                    Thread.sleep(3000); // Delay
                    lobbySpace.put("START_GAME", "ALL");
                    System.out.println("START_GAME signal sent to all players.");
                } catch (InterruptedException e) {
                    System.err.println("Error during game start delay: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
