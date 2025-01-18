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

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LobbyClient extends Application {
    private static final String SERVER_URI = "tcp://127.0.0.1:9001/lobby?keep";

    private RemoteSpace lobbySpace;
    private final ListView<String> playerListView = new ListView<>();
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

            // Start polling the space for updates and START_GAME signal
            startPollingUpdates();

        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            showError("Failed to connect to server. Ensure it is running and reachable at " + SERVER_URI);
        }
    }

    /**
     * Periodically checks for:
     *   1) ALL ("UPDATE", somePlayer, isReady) tuples (non-destructive read)
     *   2) Then a ("START_GAME", thisPlayerName) tuple (destructive read)
     */
    private void startPollingUpdates() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // Poll every 200ms instead of every second
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 1) Non-destructive read of all "UPDATE"
                List<Object[]> allUpdates = lobbySpace.queryAll(
                        new ActualField("UPDATE"),
                        new FormalField(String.class),
                        new FormalField(Boolean.class)
                );

                // Rebuild the UI with the new snapshot
                Platform.runLater(() -> refreshPlayerList(allUpdates));

                // 2) Destructive read for "START_GAME" (unique to this player)
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
        }, 0, 200, TimeUnit.MILLISECONDS); // <-- Poll every 200ms
    }


    /**
     * Clear the player list and re-add all current player statuses from "UPDATE" tuples.
     */
    private void refreshPlayerList(List<Object[]> allUpdates) {
        // Clear the old list
        playerListView.getItems().clear();

        // Rebuild the entire list
        for (Object[] update : allUpdates) {
            String type = (String) update[0];          // "UPDATE"
            String updatedPlayer = (String) update[1];
            Boolean isReadyStatus = (Boolean) update[2];

            if ("UPDATE".equals(type)) {
                String displayName = updatedPlayer.equals(playerName)
                        ? "Me (" + updatedPlayer + ")"
                        : updatedPlayer;

                String statusString = displayName + (isReadyStatus ? " (Ready)" : " (Not Ready)");
                playerListView.getItems().add(statusString);

                //System.out.println("Refreshed player: " + statusString);
            }
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

            // Send the action to the server
            String action = ready ? "READY" : "NOT_READY";
            lobbySpace.put(playerName, action);

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
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game Started!");
        alert.setHeaderText(null);
        alert.setContentText("The game is starting now for " + playerName + "!");
        alert.showAndWait();

        // e.g. close the lobby window if desired:
        // Stage stage = (Stage) readyButton.getScene().getWindow();
        // stage.close();
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
