package org.example.server;

import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;
import org.jspace.RemoteSpace;

import java.util.ArrayList;
import java.util.List;

public class TankServer {
    private static final String LOBBY_URI = "tcp://127.0.0.1:9001/lobby?keep";

    private final Space gameSpace;
    private RemoteSpace lobbySpace;
    private final List<String> connectedPlayers = new ArrayList<>();

    public TankServer(Space gameSpace) {
        this.gameSpace = gameSpace;
        try {
            lobbySpace = new RemoteSpace(LOBBY_URI);
            System.out.println("Connected to lobby at " + LOBBY_URI);
        } catch (Exception e) {
            System.err.println("Failed to connect to LobbyServer: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void start() {
        System.out.println("Waiting for START_GAME signal for the server...");

        try {
            // Listen specifically for ("START_GAME", "SERVER")
            Object[] startSignal = lobbySpace.get(
                    new org.jspace.ActualField("START_GAME"),
                    new org.jspace.ActualField("SERVER")
            );
            // Once we get that tuple, we know it's time to start the game
            System.out.println("TankServer received START_GAME signal for: " + startSignal[1]);

            // Start the game loop once the signal is received
            startGameLoop();
        } catch (InterruptedException e) {
            System.err.println("Error while waiting for START_GAME signal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startGameLoop() {
        System.out.println("Starting the game loop...");
        Simulation simulation = new Simulation(gameSpace, connectedPlayers);
        simulation.run();
    }

    public static void main(String[] args) throws Exception {
        Space gameSpace = new SequentialSpace();

        TankServer server = new TankServer(gameSpace);
        new Thread(server::start).start();

        SpaceRepository repository = new SpaceRepository();
        repository.add("game", gameSpace);

        repository.addGate("tcp://0.0.0.0:9002/?keep");
        System.out.println("TankServer is running on tcp://0.0.0.0:9002");
    }
}
