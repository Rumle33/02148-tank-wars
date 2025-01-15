package org.example.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class LobbyServer {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("LobbyServer started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Broadcasts the list of players and their ready status to all clients
    public static void broadcastUpdate() {
        StringBuilder updateMessage = new StringBuilder("UPDATE\n");
        int readyCount = 0;
        for (ClientHandler client : clients) {
            updateMessage.append(client.getPlayerName()).append(" - ")
                    .append(client.isReady() ? "Ready" : "Not Ready").append("\n");
            if (client.isReady()) {
                readyCount++;
            }
        }

        System.out.println("Broadcasting update:\n" + updateMessage); // Debugging line

        for (ClientHandler client : clients) {
            client.sendMessage(updateMessage.toString());
        }

        // Check if exactly two players are ready to start the game
        // Check if exactly two players are ready to start the game
        if (readyCount == 2) {
            System.out.println("Two players ready. Starting game...");
            for (ClientHandler client : clients) {
                client.sendMessage("START");
            }
        }
    }

    // Inner class to handle each client connection
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String playerName;
        private boolean ready = false;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getPlayerName() {
            return playerName;
        }

        public boolean isReady() {
            return ready;
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // First message from client is the player name
                playerName = in.readLine();
                System.out.println(playerName + " joined the lobby.");
                broadcastUpdate();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("READY")) {
                        ready = !ready; // Toggle ready status
                        broadcastUpdate();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clients.remove(this);
                broadcastUpdate();
            }
        }
    }
}
