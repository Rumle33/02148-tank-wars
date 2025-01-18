package org.example.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.jspace.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LobbyClient extends Application {
    private static final String SERVER_URI = "tcp://127.0.0.1:9001/lobby?keep";

    private RemoteSpace lobbySpace;
    private final ListView<String> playerListView = new ListView<>();

    /**
     * Keep track of each player's 'ready' status in a stable (insertion) order
     * so they don't jump around in the list. Because we receive the entire snapshot,
     * we can just replace localPlayers with the server's snapshot each time.
     */
    private final Map<String, Boolean> localPlayers = new LinkedHashMap<>();

    private boolean ready = false;
    private final Button readyButton = new Button("Not Ready");
    private String playerName;
    private ScheduledExecutorService scheduler;

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label nameLabel = new Label("Enter your player name:");
        TextField nameField = new TextField();
        Button joinButton = new Button("Join Lobby");

        Label lobbyLabel = new Label("Lobby - Connected Players");
        readyButton.setStyle("-fx-background-color: red;");
        readyButton.setDisable(true); // disabled until we join

        readyButton.setOnAction(e -> toggleReadyStatus());

        root.getChildren().addAll(nameLabel, nameField, joinButton);

        Scene scene = new Scene(root, 300, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Tank Game Lobby");
        primaryStage.show();

        // Handle the "Join Lobby" button
        joinButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                playerName = name;

                // Clear the UI and show the lobby interface
                root.getChildren().clear();
                root.getChildren().addAll(lobbyLabel, playerListView, readyButton);
                readyButton.setDisable(false);

                connectToServer();
            } else {
                showError("Player name cannot be empty!");
            }
        });
    }

    /**
     * Connects to the lobby server and signals JOIN.
     */
    private void connectToServer() {
        try {
            System.out.println("Attempting to connect to server at " + SERVER_URI);
            lobbySpace = new RemoteSpace(SERVER_URI);
            System.out.println("Connection established to " + SERVER_URI);

            // Send JOIN action
            lobbySpace.put(playerName, "JOIN");
            System.out.println("Connected to server as " + playerName);

            // Start polling the space for updates (UPDATE_ALL) and START_GAME signals
            startPollingUpdates();

        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            showError("Failed to connect to server. Ensure it is running and reachable at " + SERVER_URI);
        }
    }

    /**
     * Periodically checks for:
     *   1) ALL ("UPDATE_ALL", Map<String, Boolean>) tuples
     *   2) Then a ("START_GAME", thisPlayerName) tuple
     */
    private void startPollingUpdates() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // Poll more frequently, e.g., every 200 ms to match LobbyServer's broadcast
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 1) Look (non-blocking) for the "UPDATE_ALL" tuple WITHOUT removing it
                Object[] updateAll = lobbySpace.queryp(
                        new ActualField("UPDATE_ALL"),
                        new FormalField(Object.class)
                );
                if (updateAll != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Boolean> snapshot = (Map<String, Boolean>) updateAll[1];

                    // Update localPlayers with the entire snapshot
                    localPlayers.clear();
                    localPlayers.putAll(snapshot);

                    Platform.runLater(this::refreshPlayerList);
                }

                // 2) Check for "START_GAME" tuple for this player (still getp(...) because each client
                //    should remove its own start signal)
                Object[] startGameTuple = lobbySpace.getp(
                        new ActualField("START_GAME"),
                        new ActualField(playerName)
                );
                if (startGameTuple != null) {
                    Platform.runLater(this::handleStartGame);
                }

            } catch (Exception e) {
                System.err.println("Error polling updates: " + e.getMessage());
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
    }

        /**
         * Rebuilds the playerListView from the localPlayers map in stable order.
         * This ensures each player stays in the same index (i.e., insertion order).
         */
    private void refreshPlayerList() {
        // Clear the old items
        playerListView.getItems().clear();

        // Iterate in stable insertion order
        for (Map.Entry<String, Boolean> entry : localPlayers.entrySet()) {
            String pName = entry.getKey();
            Boolean isReady = entry.getValue();

            // If this is the local player, display "Me (pName)"
            String displayName = pName.equals(playerName)
                    ? "Me (" + pName + ")"
                    : pName;

            String status = displayName + (isReady ? " (Ready)" : " (Not Ready)");
            playerListView.getItems().add(status);
        }
    }

    /**
     * Toggles the ready state for this player and updates the server.
     */
    private void toggleReadyStatus() {
        try {
            ready = !ready;
            readyButton.setText(ready ? "Ready" : "Not Ready");
            readyButton.setStyle(ready ? "-fx-background-color: green;" : "-fx-background-color: red;");

            // Inform the server
            lobbySpace.put(playerName, ready ? "READY" : "NOT_READY");
            System.out.println(playerName + " is now " + (ready ? "ready" : "not ready"));
        } catch (InterruptedException e) {
            showError("Error toggling ready status: " + e.getMessage());
        }
    }

    /**
     * Called once we detect ("START_GAME", playerName).
     * Here you can transition to your game scene or close the lobby UI.
     */
    private void handleStartGame() {
        // 1) Close the lobby UI
        Stage lobbyStage = (Stage) readyButton.getScene().getWindow();
        lobbyStage.close();

        // 2) Connect to the TankServer's game space
        try {
            RemoteSpace gameSpace = new RemoteSpace("tcp://127.0.0.1:9002/game?keep");
            System.out.println("LobbyClient connected to the game space on port 9002");

            // 3) (Optional) Launch a "TankGame" scene,
            //    or pass 'gameSpace' to your game logic class:
            Stage gameStage = new Stage();
            gameStage.setTitle("Tank Game - " + playerName);

            // Example minimal scene:
            Label info = new Label("Player " + playerName + " is now in the game!");
            Scene gameScene = new Scene(new VBox(info), 400, 300);
            gameStage.setScene(gameScene);
            gameStage.show();

            // In a real scenario, you'd keep 'gameSpace' to do further communications,
            // e.g. TankGame simulation calls.

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to connect to game space: " + e.getMessage());
        }
    }


    /**
     * Shows an error message in a JavaFX Alert.
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Called when the application is about to exit.
     * We notify the server that this player has left the lobby.
     */
    @Override
    public void stop() {
        // Attempt to send a LEAVE action
        try {
            if (lobbySpace != null && playerName != null && !playerName.isEmpty()) {
                lobbySpace.put(playerName, "LEAVE");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        stopPollingUpdates();
    }

    /**
     * Properly shuts down the scheduler that polls for lobby updates.
     */
    private void stopPollingUpdates() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting LobbyClient with SERVER_URI: " + SERVER_URI);
        launch(args);
    }
}
