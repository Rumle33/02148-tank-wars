package org.example.Tank;

import org.example.server.LobbyClient;
import org.example.server.TankServer;
import org.jspace.SequentialSpace;
import org.jspace.Space;

public class Main {
    public static void main(String[] args) {
        try {
            // Launch the LobbyClient in a separate thread
            Thread clientThread = new Thread(() -> LobbyClient.main(args));
            clientThread.start();

            // Prepare the game space for the TankServer
            Space gameSpace = new SequentialSpace();

            // Start the TankServer after receiving the START_GAME signal
            TankServer server = new TankServer(gameSpace);
            System.out.println("Waiting for the game to start...");
            server.start(); // TankServer waits for the START_GAME signal from the LobbyServer
        } catch (Exception e) {
            System.err.println("Error in Main: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
