package org.example.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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
    private final TextArea chatArea = new TextArea();
    private final TextField chatInput = new TextField();
    private final Button sendButton = new Button("Send");

    private boolean ready = false;
    private final Button readyButton = new Button("Not Ready");
    private String playerName;
    private ScheduledExecutorService scheduler;

    // Adjust the client poll interval
    private static final int POLL_INTERVAL_MS = 300;

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label nameLabel = new Label("Enter your player name:");
        TextField nameField = new TextField();
        Button joinButton = new Button("Join Lobby");

        // Initially hide the lobby UI (we'll show it after join)
        VBox lobbyUI = new VBox(10);
        Label lobbyLabel = new Label("Lobby - Connected Players");
        readyButton.setStyle("-fx-background-color: red;");
        readyButton.setDisable(true); // disabled until we join

        // Set up chat
        chatArea.setEditable(false);
        chatArea.setPrefHeight(200);
        HBox chatControls = new HBox(5, chatInput, sendButton);
        chatControls.setPrefHeight(30);

        // Put the player list, ready button, chat area, chat input all together
        lobbyUI.getChildren().addAll(lobbyLabel, playerListView, readyButton, new Label("Lobby Chat:"), chatArea, chatControls);

        // We'll initially show just the name input
        root.getChildren().addAll(nameLabel, nameField, joinButton);

        Scene scene = new Scene(root, 400, 500);
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
                root.getChildren().add(lobbyUI);
                readyButton.setDisable(false);

                connectToServer();
            } else {
                showError("Player name cannot be empty!");
            }
        });

        // Toggle ready on button click
        readyButton.setOnAction(e -> toggleReadyStatus());

        // Send chat messages on button click
        sendButton.setOnAction(e -> sendChatMessage());
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
            showError("Failed to connect to server. Ensure it is running at " + SERVER_URI);
        }
    }

    /**
     * Periodically reads:
     *   1) ALL ("UPDATE", somePlayer, isReady) tuples (non-destructive)
     *   2) ALL ("CHAT_MSG", somePlayer, message) tuples (non-destructive)
     *   3) Then a "START_GAME" (thisPlayer) destructively
     */
    private void startPollingUpdates() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 1) Non-destructive read of all "UPDATE"
                List<Object[]> allUpdates = lobbySpace.queryAll(
                        new ActualField("UPDATE"),
                        new FormalField(String.class),
                        new FormalField(Boolean.class)
                );

                // 2) Non-destructive read of all "CHAT_MSG"
                List<Object[]> allChats = lobbySpace.queryAll(
                        new ActualField("CHAT_MSG"),
                        new FormalField(String.class),  // sender
                        new FormalField(String.class)   // message
                );

                // 3) Destructive read for START_GAME for this player
                Object[] startGameTuple = lobbySpace.getp(
                        new ActualField("START_GAME"),
                        new ActualField(playerName)
                );

                // Update the UI in the JavaFX thread
                Platform.runLater(() -> {
                    refreshPlayerList(allUpdates);
                    refreshChat(allChats);

                    if (startGameTuple != null) {
                        handleStartGame();
                    }
                });

            } catch (Exception e) {
                System.err.println("Error polling updates: " + e.getMessage());
            }
        }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Clear and rebuild the entire player list from "UPDATE" tuples.
     */
    private void refreshPlayerList(List<Object[]> allUpdates) {
        playerListView.getItems().clear();
        for (Object[] tuple : allUpdates) {
            String type = (String) tuple[0]; // "UPDATE"
            String updatedPlayer = (String) tuple[1];
            Boolean isReadyStatus = (Boolean) tuple[2];

            if ("UPDATE".equals(type)) {
                String displayName = updatedPlayer.equals(playerName)
                        ? "Me (" + updatedPlayer + ")"
                        : updatedPlayer;
                String status = displayName + (isReadyStatus ? " (Ready)" : " (Not Ready)");
                playerListView.getItems().add(status);
            }
        }
    }

    /**
     * Clear and rebuild the chat area from "CHAT_MSG" tuples.
     */
    private void refreshChat(List<Object[]> allChats) {
        chatArea.clear();
        // Sort by time? (In jSpace, there's no timestamp by default.)
        // We'll just display them in the order we get them:
        for (Object[] tuple : allChats) {
            String type = (String) tuple[0];  // "CHAT_MSG"
            String sender = (String) tuple[1];
            String message = (String) tuple[2];

            if ("CHAT_MSG".equals(type)) {
                chatArea.appendText(sender + ": " + message + "\n");
            }
        }
    }

    /**
     * Toggles the ready state for this player and notifies the server.
     */
    private void toggleReadyStatus() {
        try {
            ready = !ready;
            readyButton.setText(ready ? "Ready" : "Not Ready");
            readyButton.setStyle(ready ? "-fx-background-color: green;" : "-fx-background-color: red;");

            lobbySpace.put(playerName, ready ? "READY" : "NOT_READY");
            System.out.println(playerName + " is now " + (ready ? "ready" : "not ready"));
        } catch (InterruptedException e) {
            showError("Error toggling ready status: " + e.getMessage());
        }
    }

    /**
     * Sends a chat message as ("CHAT_MSG", playerName, text).
     */
    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty()) {
            try {
                // Put a 3-field tuple for chat
                lobbySpace.put("CHAT_MSG", playerName, text);
                chatInput.clear();
            } catch (InterruptedException e) {
                showError("Error sending chat message: " + e.getMessage());
            }
        }
    }

    /**
     * Called once we detect ("START_GAME", playerName).
     * Close the lobby or transition to the game scene.
     */
    private void handleStartGame() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game Started!");
        alert.setHeaderText(null);
        alert.setContentText("The game is starting now for " + playerName + "!");
        alert.showAndWait();

        // Option 1: Close the lobby window
        Stage stage = (Stage) playerListView.getScene().getWindow();
        stage.close();

        // Option 2: Or just hide chat controls if you prefer, e.g.:
        // chatArea.setVisible(false);
        // chatInput.setVisible(false);
        // sendButton.setVisible(false);
        // readyButton.setVisible(false);
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
