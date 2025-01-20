package org.example.server;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.Tank.Tank;
import org.jspace.*;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LobbyClient {
    // Spaces
    private RemoteSpace lobbySpace;
    private RemoteSpace gameSpace;

    // UI
    private final ListView<String> playerListView = new ListView<>();
    private final TextArea chatArea = new TextArea();
    private final TextField chatInput = new TextField();
    private final Button sendButton = new Button("Send");
    private final Button readyButton = new Button("Not Ready");

    // State
    private boolean ready = false;
    private String playerName;
    private ScheduledExecutorService scheduler;

    // Adjust the client poll interval
    private static final int POLL_INTERVAL_MS = 300;

    /**
     * Provide these two set methods so you can pass the spaces from outside:
     */
    public void setLobbySpace(RemoteSpace lobbySpace) {
        this.lobbySpace = lobbySpace;
    }

    public void setGameSpace(RemoteSpace gameSpace) {
        this.gameSpace = gameSpace;
    }

    /**
     * Build and return the entire Lobby scene. You call this once you have set
     * the lobbySpace/gameSpace and the player's name.
     */
    public Scene createLobbyScene(String playerName) {
        this.playerName = playerName;

        // Outer layout
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // The top label and readiness button
        Label lobbyLabel = new Label("Lobby - Connected Players");
        readyButton.setStyle("-fx-background-color: red;");
        readyButton.setDisable(false); // enabled after we "join"
        readyButton.setOnAction(e -> toggleReadyStatus());

        // Chat area
        chatArea.setEditable(false);
        chatArea.setPrefHeight(200);

        HBox chatControls = new HBox(5, chatInput, sendButton);
        chatControls.setPrefHeight(30);
        sendButton.setOnAction(e -> sendChatMessage());

        // Put it all together
        root.getChildren().addAll(
                lobbyLabel,
                playerListView,
                readyButton,
                new Label("Lobby Chat:"),
                chatArea,
                chatControls
        );

        Scene scene = new Scene(root, 400, 500);

        // Actually join the lobby (put the JOIN tuple) and start polling
        connectAndStartPolling();

        return scene;
    }

    /**
     * Connect to the spaces by putting (playerName, "JOIN"), then start
     * a background thread to poll for updates & "START_GAME" signals.
     */
    private void connectAndStartPolling() {
        try {
            // Send JOIN action to the lobby
            lobbySpace.put("LOBBY", "JOIN", playerName);
            System.out.println("Connected to server as " + playerName);

            startPollingUpdates();

        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            showError("Failed to connect to server. Ensure it is running.");
        }
    }

    /**
     * Periodically reads:
     *   1) ALL ("UPDATE", somePlayer, isReady) tuples (non-destructive)
     *   2) ALL ("CHAT_MSG", somePlayer, message) tuples (non-destructive)
     *   3) Then a "START_GAME" (thisPlayer) destructively from the game space
     */
    private void startPollingUpdates() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // (1) Non-destructive read of all "UPDATE"
                List<Object[]> allUpdates = lobbySpace.queryAll(
                        new ActualField("LOBBY"),
                        new ActualField("UPDATE"),
                        new FormalField(String.class),
                        new FormalField(Boolean.class)
                );

                // (2) Non-destructive read of all "CHAT_MSG"
                List<Object[]> allChats = lobbySpace.queryAll(
                        new ActualField("LOBBY"),
                        new ActualField("CHAT_MSG"),
                        new FormalField(String.class),
                        new FormalField(String.class)
                );

                // (3) Destructive read for "START_GAME" for this player
                Object[] startGameTuple = lobbySpace.getp(
                        new ActualField("START_GAME"),
                        new ActualField(playerName)
                );

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
     * Update the player list from "UPDATE" tuples.
     */
    private void refreshPlayerList(List<Object[]> allUpdates) {
        playerListView.getItems().clear();
        for (Object[] tuple : allUpdates) {
            String type = (String) tuple[1]; // "UPDATE"
            String updatedPlayer = (String) tuple[2];
            Boolean isReadyStatus = (Boolean) tuple[3];

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
     * Update the chat area from "CHAT_MSG" tuples.
     */
    private void refreshChat(List<Object[]> allChats) {
        chatArea.clear();
        for (Object[] tuple : allChats) {
            String type = (String) tuple[1];  // "CHAT_MSG"
            String sender = (String) tuple[2];
            String message = (String) tuple[3];

            if ("CHAT_MSG".equals(type)) {
                chatArea.appendText(sender + ": " + message + "\n");
            }
        }
    }

    /**
     * Toggle ready status and inform server
     */
    private void toggleReadyStatus() {
        try {
            ready = !ready;
            readyButton.setText(ready ? "Ready" : "Not Ready");
            readyButton.setStyle(ready ? "-fx-background-color: green;" : "-fx-background-color: red;");

            lobbySpace.put("LOBBY", ready ? "READY" : "NOT_READY", playerName);
            System.out.println(playerName + " is now " + (ready ? "ready" : "not ready"));
        } catch (InterruptedException e) {
            showError("Error toggling ready status: " + e.getMessage());
        }
    }

    /**
     * Send chat message as ("CHAT_MSG", playerName, text)
     */
    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty()) {
            try {
                lobbySpace.put("LOBBY", "CHAT_MSG", playerName, text);
                chatInput.clear();
            } catch (InterruptedException e) {
                showError("Error sending chat message: " + e.getMessage());
            }
        }
    }

    /**
     * Received "START_GAME" for this player.
     * You can close the lobby UI and open your game scene from here.
     */
    private void handleStartGame() {
        System.out.println("[LOBBY CLIENT] START_GAME signal received for player: " + playerName);

        // Stop the polling thread and build your real game UI
        stopPollingUpdates();

        Platform.runLater(() -> {
            // 1) Close the current lobby stage
            Stage lobbyStage = (Stage) playerListView.getScene().getWindow();
            lobbyStage.close();

            // 2) Create a new Stage for the game
            Stage gameStage = new Stage();

            // 3) Create a new root Pane (or any layout node)
            Pane root = new Pane();

            // 4) Instantiate and configure your Tank
            Tank tank = new Tank();
            tank.setLobbySpace(lobbySpace);
            tank.setGameSpace(gameSpace);
            tank.setRoot(root);

            // 5) Start the Tank game on the new game stage
            tank.start(gameStage);
        });
    }

    /**
     * Show error in UI
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
     * Gracefully stop the polling thread
     */
    private void stopPollingUpdates() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * Call this when closing the lobby so the server knows you left.
     */
    public void leaveLobby() {
        try {
            if (lobbySpace != null && playerName != null && !playerName.isEmpty()) {
                lobbySpace.put("LOBBY", "LEAVE", playerName);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopPollingUpdates();
    }
}
