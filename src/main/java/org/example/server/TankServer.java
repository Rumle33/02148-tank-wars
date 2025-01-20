package org.example.server;

import org.jspace.ActualField;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

import java.util.ArrayList;
import java.util.List;

public class TankServer {
    private static final int MAX_PLAYERS = 2;
    private final Space lobbySpace;
    private final Space gameSpace;
    private final List<String> connectedPlayers = new ArrayList<>();

    public TankServer(Space lobbySpace, Space gameSpace) {
        this.lobbySpace = lobbySpace;
        this.gameSpace = gameSpace;
    }

    public void start() {
		
        try {
			lobbySpace.get(new ActualField("START_SERVER"));
			while (connectedPlayers.size() < MAX_PLAYERS) {
				System.out.println("[TANK SERVER] waiting for players...");
                Object[] playerInfo = gameSpace.get(new org.jspace.ActualField("JOIN"), new org.jspace.FormalField(String.class));
                String playerName = (String) playerInfo[1];

                if (!connectedPlayers.contains(playerName)) {
                    connectedPlayers.add(playerName);
                    System.out.println("[TANK SERVER] Player joined: " + playerName + ". Total players: " + connectedPlayers.size());
                    gameSpace.put("PLAYER_JOINED", playerName, connectedPlayers.size());
                } else {
                    System.out.println("[TANK SERVER] Duplicate player name: " + playerName);
                    gameSpace.put("ERROR", "Name already taken. Choose another.");
                }
            }

            System.out.println("[TANK SERVER] All players joined! Starting the game...");
            for (String player : connectedPlayers) {
                gameSpace.put("START_GAME", player);
            }
            startGameLoop();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startGameLoop() {
        Simulation simulation = new Simulation(gameSpace, connectedPlayers);
        simulation.run();
    }

    /*
    public static void main(String[] args) throws Exception {
        Space lobbySpace = new SequentialSpace();
        Space gameSpace = new SequentialSpace();

        TankServer server = new TankServer(lobbySpace, gameSpace);
        new Thread(server::start).start();

        SpaceRepository repository = new SpaceRepository();
        repository.add("lobby", lobbySpace);
        repository.add("game", gameSpace);

        repository.addGate("tcp://localhost:12345/?keep");
        System.out.println("Server is running on localhost:12345...");
    }

     */
}