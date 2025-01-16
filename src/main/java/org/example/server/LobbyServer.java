package org.example.server;

import org.jspace.*;
import java.util.*;

public class LobbyServer {
    private static final String LOBBY_URI = "tcp://localhost:9001/lobby?keep";
    private static SpaceRepository repository;
    private static Space lobbySpace;
    private static int readyCount = 0;
    private static List<String> players = new ArrayList<>();

    public static void main(String[] args) {
        repository = new SpaceRepository();
        lobbySpace = new SequentialSpace();
        repository.add("lobby", lobbySpace);

        repository.addGate("tcp://localhost:9001/?keep");
        System.out.println("LobbyServer started and listening on " + LOBBY_URI);

        new Thread(() -> handleLobby()).start();
    }

    private static void handleLobby() {
        try {
            while (true) {
                Object[] tuple = lobbySpace.get(new FormalField(String.class), new FormalField(String.class));
                String playerName = (String) tuple[0];
                String action = (String) tuple[1];

                synchronized (players) {
                    if (action.equals("JOIN")) {
                        players.add(playerName);
                        System.out.println(playerName + " joined the lobby.");
                        updatePlayerList();
                    } else if (action.equals("LEAVE")) {
                        players.remove(playerName);
                        System.out.println(playerName + " left the lobby.");
                        updatePlayerList();
                    } else if (action.equals("READY")) {
                        readyCount++;
                        System.out.println(playerName + " is ready.");
                        checkGameStart();
                    } else if (action.equals("NOT_READY")) {
                        readyCount--;
                        System.out.println(playerName + " is not ready.");
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void updatePlayerList() {
        try {
            String playerList = String.join(",", players);
            System.out.println("Sending player list update: " + playerList); // Debug
            lobbySpace.put("UPDATE", playerList);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void checkGameStart() {
        synchronized (players) {
            if (readyCount == players.size() && readyCount > 0) {
                System.out.println("All players are ready. Starting the game!");
                try {
                    lobbySpace.put("START_GAME", "ALL");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                players.clear();
                readyCount = 0;
            }
        }
    }
}