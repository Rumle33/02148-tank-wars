package org.example.server;

import org.jspace.*;
import java.util.*;

public class LobbyServer {
    private static final String LOBBY_URI = "tcp://127.0.0.1:9001/lobby?keep";
    private static final String GAME_URI = "tcp://127.0.0.1:9002/game?keep";

    private static SpaceRepository repository;
    private static Space lobbySpace;
    private static Space gameSpace;
    private static final Map<String, Boolean> players = new HashMap<>();
    private static boolean gameStarted = false; // Flag to prevent multiple triggers

    // Speed up broadcast so clients see changes quickly
    private static final int BROADCAST_DELAY_MS = 300;

    public static void main(String[] args) {
        repository = new SpaceRepository();
        lobbySpace = new SequentialSpace();
        gameSpace = new SequentialSpace();
        repository.add("lobby", lobbySpace);
        repository.add("game", gameSpace);

        try {
            // Open the TCP gates
            repository.addGate("tcp://127.0.0.1:9001/?keep");
            repository.addGate("tcp://127.0.0.1:9002/?keep");
            System.out.println("LobbyServer started on " + LOBBY_URI);
            System.out.println("GameServer started on " + GAME_URI);

            // 1) Clear out any old data in case of leftover from previous runs
            lobbySpace.getAll(new ActualField("UPDATE"), new FormalField(String.class), new FormalField(Boolean.class));
            gameSpace.getAll(new ActualField("START_GAME"), new FormalField(String.class));
            // Remove any old chat messages
            lobbySpace.getAll(new ActualField("CHAT_MSG"), new FormalField(String.class), new FormalField(String.class));
            System.out.println("Cleaned up old tuples in the lobby space.");

        } catch (Exception e) {
            System.err.println("Error opening gates: " + e.getMessage());
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
                            Thread.sleep(3000);

                            synchronized (players) {
                                // Debugging information
                                System.out.println("Preparing to start game between: " + readyPlayers);

                                // Remove players from the lobby
                                players.remove(readyPlayers.get(0));
                                players.remove(readyPlayers.get(1));

                                // Remove all chat messages before the game starts (optional)
                                lobbySpace.getAll(
                                        new ActualField("CHAT_MSG"),
                                        new FormalField(String.class),
                                        new FormalField(String.class)
                                );
                                System.out.println("Removed all chat messages from the lobby.");

                                // Send START_GAME to the game space
                                gameSpace.put("START_GAME", readyPlayers.get(0));
                                gameSpace.put("START_GAME", readyPlayers.get(1));
                                System.out.println("START_GAME signal sent for players: " + readyPlayers);

                                // Reset the flag and debug information
                                gameStarted = false;
                                System.out.println("Game started successfully. Flag reset.");
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
