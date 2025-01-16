package org.example.server;

import org.jspace.*;
import java.util.*;

public class LobbyServer {
    // URI for the lobby space server
    private static final String LOBBY_URI = "tcp://localhost:9001/lobby?keep";

    // Repository for spaces
    private static SpaceRepository repository;

    // Space for lobby communication
    private static Space lobbySpace;

    // Map to store players and their readiness state
    private static final Map<String, Boolean> players = new HashMap<>();

    public static void main(String[] args) {
        // Initialize repository and lobby space
        repository = new SpaceRepository();
        lobbySpace = new SequentialSpace();
        repository.add("lobby", lobbySpace);

        try {
            // Open a gate for the lobby space
            repository.addGate("tcp://localhost:9001/?keep");
            System.out.println("LobbyServer started and listening on " + LOBBY_URI);
        } catch (Exception e) {
            // Handle errors when opening the gate
            System.err.println("Error opening gate: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Start a new thread to handle lobby actions
        new Thread(LobbyServer::handleLobby).start();
    }

    // Method to handle player actions in the lobby
    private static void handleLobby() {
        try {
            while (true) {
                System.out.println("Waiting for player actions...");

                // Receive a tuple from the lobby space: playerName and action
                Object[] tuple = lobbySpace.get(new FormalField(String.class), new FormalField(String.class));
                String playerName = (String) tuple[0];
                String action = (String) tuple[1];
                System.out.println("Received action: " + action + " from player: " + playerName);

                synchronized (players) {
                    // Handle different player actions
                    switch (action) {
                        case "JOIN":
                            players.put(playerName, false); // Add player as not ready
                            System.out.println(playerName + " joined the lobby.");
                            break;
                        case "LEAVE":
                            players.remove(playerName); // Remove player from the lobby
                            System.out.println(playerName + " left the lobby.");
                            break;
                        case "READY":
                            players.put(playerName, true); // Mark player as ready
                            System.out.println(playerName + " is ready.");
                            break;
                        case "NOT_READY":
                            players.put(playerName, false); // Mark player as not ready
                            System.out.println(playerName + " is not ready.");
                            break;
                        default:
                            // Handle unknown actions
                            System.err.println("Unknown action: " + action);
                            break;
                    }
                    // Update the player list in the lobby
                    updatePlayerList();
                    // Check if the game can start
                    checkGameStart();
                }
            }
        } catch (InterruptedException e) {
            // Handle errors in the lobby handling loop
            System.err.println("Error in handleLobby: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to update the player list and notify clients
    private static void updatePlayerList() {
        try {
            StringBuilder playerList = new StringBuilder();
            for (Map.Entry<String, Boolean> entry : players.entrySet()) {
                playerList.append(entry.getKey())
                        .append(entry.getValue() ? " (Ready)" : " (Not Ready)")
                        .append(",");
            }
            if (playerList.length() > 0) {
                playerList.setLength(playerList.length() - 1); // Remove trailing comma
            }

            // Put the updated player list into the lobby space
            lobbySpace.put("UPDATE", playerList.toString());
            System.out.println("Players updated: " + players.keySet());
        } catch (InterruptedException e) {
            // Handle errors during player list update
            System.err.println("Error updating player list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to check if all players are ready to start the game
    private static void checkGameStart() {
        if (players.values().stream().allMatch(ready -> ready)) {
            // All players are ready; start the game
            System.out.println("All players are ready. Starting the game!");
            try {
                lobbySpace.put("START_GAME", "ALL");
            } catch (InterruptedException e) {
                // Handle errors during game start
                System.err.println("Error starting game: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
