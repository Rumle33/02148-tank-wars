package org.example.server;

import org.jspace.*;
import java.util.*;

public class LobbyServer {

    private static Space lobbySpace;
    private static Space gameSpace;
    private static final Map<String, Boolean> players = new HashMap<>();
    private static boolean gameStarted = false; // Flag to prevent multiple triggers

    // Speed up broadcast so clients see changes quickly
    private static final int BROADCAST_DELAY_MS = 300;

    public LobbyServer(Space lobbySpace, Space gameSpace) {
        this.lobbySpace = lobbySpace;
        this.gameSpace = gameSpace;
    }


    /**
     * Continuously wait for player actions: JOIN, LEAVE, READY, NOT_READY.
     * (Does NOT handle CHAT_MSG, so those remain in the space.)
     */
    public void handleLobby() {
        try {
            while (true) {
                System.out.println("Waiting for player actions...");
                // Wait for a tuple of shape (String, String)
                Object[] tuple = lobbySpace.get(new ActualField("LOBBY"), new FormalField(String.class), new FormalField(String.class));
                String action = (String) tuple[1];
                String playerName = (String) tuple[2];

                System.out.println("Received action: " + action + " from: " + playerName);

                synchronized (players) {
                    switch (action) {
                        case "JOIN":
                            players.put(playerName, false);
                            System.out.println(playerName + " joined the lobby. Current players: " + players);
                            break;

                        case "LEAVE":
                            players.remove(playerName);
                            System.out.println(playerName + " left the lobby. Current players: " + players);
                            // Remove that player's "UPDATE" tuple so they vanish from clients
                            lobbySpace.getAll(
                                    new ActualField("UPDATE"),
                                    new ActualField(playerName),
                                    new FormalField(Boolean.class)
                            );
                            break;

                        case "READY":
                            players.put(playerName, true);
                            System.out.println(playerName + " is ready. Current players: " + players);
                            break;

                        case "NOT_READY":
                            players.put(playerName, false);
                            System.out.println(playerName + " is not ready. Current players: " + players);
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
    public void broadcastUpdates() {
        while (true) {
            try {
                Thread.sleep(BROADCAST_DELAY_MS);

                synchronized (players) {
                    // For each known player, remove old "UPDATE" then insert fresh
                    for (String name : players.keySet()) {
                        lobbySpace.getp(new ActualField("LOBBY"), new ActualField("UPDATE"), new ActualField(name), new FormalField(Boolean.class));
                        Boolean isReady = players.get(name);
                        lobbySpace.put("LOBBY", "UPDATE", name, isReady);
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("Error broadcasting updates: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * If exactly two players are ready, start the game for them.
     */
    private static void checkGameStart() {
        if (!gameStarted) {
            List<String> readyPlayers = new ArrayList<>();

            synchronized (players) {
                for (Map.Entry<String, Boolean> entry : players.entrySet()) {
                    if (entry.getValue()) {
                        readyPlayers.add(entry.getKey());
                    }
                }

                System.out.println("Current ready players: " + readyPlayers);

                if (readyPlayers.size() == 2) {
                    gameStarted = true;
                    System.out.println("Two players ready! Starting game in 3 seconds...");

                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);

                            synchronized (players) {
                                // Debugging information
                                System.out.println("[LOBBY SERVER] Preparing to start game between: " + readyPlayers);

                                // Remove players from the lobby
                                players.remove(readyPlayers.get(0));
                                players.remove(readyPlayers.get(1));

                                // Remove all chat messages before the game starts (optional)
                                lobbySpace.getAll(
                                        new ActualField("CHAT_MSG"),
                                        new FormalField(String.class),
                                        new FormalField(String.class)
                                );
                                System.out.println("[LOBBY SERVER] Removed all chat messages from the lobby.");

                                // Send START_GAME to the game space
                                lobbySpace.put("START_GAME", readyPlayers.get(0));
                                lobbySpace.put("START_GAME", readyPlayers.get(1));

                                System.out.println("[LOBBY SERVER] START_GAME signal sent for players: " + readyPlayers);

								lobbySpace.put("START_SERVER");

                                // Reset the flag and debug information
                                gameStarted = false;
                                System.out.println("[LOBBY SERVER] Game started successfully. Flag reset.");
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    System.out.println("Not enough ready players to start the game. Current ready players: " + readyPlayers);
                }
            }
        }
    }
}