package org.example.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.control.Alert.AlertType;
import org.jspace.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LobbyClient extends Application {
    private static final String SERVER_URI = "tcp://127.0.0.1:9001/lobby?keep";

    private RemoteSpace lobbySpace;
    private final ListView<String> playerListView = new ListView<>();
    private boolean ready = false;
    private final Button readyButton = new Button("Not Ready");
    private String playerName;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label nameLabel = new Label("Enter your player name:");
        TextField nameField = new TextField();
        Button joinButton = new Button("Join Lobby");

        Label lobbyLabel = new Label("Lobby - Connected Players");
        readyButton.setStyle("-fx-background-color: red;");
        readyButton.setOnAction(e -> toggleReadyStatus());
        readyButton.setDisable(true);

        root.getChildren().addAll(nameLabel, nameField, joinButton);

        Scene scene = new Scene(root, 300, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Tank Game Lobby");
        primaryStage.show();

        joinButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                playerName = name;
                root.getChildren().clear();
                root.getChildren().addAll(lobbyLabel, playerListView, readyButton);
                readyButton.setDisable(false);
                connectToServer();
            } else {
                showError("Player name cannot be empty!");
            }
        });
    }

    private void connectToServer() {
        try {
            System.out.println("Attempting to connect to server at " + SERVER_URI);
            lobbySpace = new RemoteSpace(SERVER_URI);
            System.out.println("Connection established to " + SERVER_URI);
            lobbySpace.put(playerName, "JOIN");
            System.out.println("Connected to server as " + playerName);
            listenForUpdates();
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            System.err.println("Please ensure the server is running on " + SERVER_URI);
            showError("Failed to connect to server. Ensure it is running and reachable at " + SERVER_URI);
        }
    }

    private void listenForUpdates() {
        executor.submit(() -> {
            try {
                while (true) {
                    Object[] update = lobbySpace.get(new FormalField(String.class),
                            new FormalField(String.class),
                            new FormalField(Boolean.class));
                    String type = (String) update[0];
                    String playerName = (String) update[1];
                    Boolean isReady = (Boolean) update[2];

                    if (type.equals("UPDATE")) {
                        Platform.runLater(() -> updatePlayerUI(playerName, isReady));
                    } else if (type.equals("START_GAME")) {
                        Platform.runLater(() -> {
                            System.out.println("All players are ready. Starting the game!");
                            Alert alert = new Alert(AlertType.INFORMATION);
                            alert.setTitle("Game Starting");
                            alert.setHeaderText(null);
                            alert.setContentText("The game will start in 3 seconds!");
                            alert.showAndWait();
                        });
                        break;
                    }
                }
            } catch (InterruptedException e) {
                showError("Error in listenForUpdates: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void updatePlayerUI(String playerName, Boolean isReady) {
        Platform.runLater(() -> {
            String status = playerName + (isReady ? " (Ready)" : " (Not Ready)");
            playerListView.getItems().removeIf(item -> item.startsWith(playerName));
            playerListView.getItems().add(status);
            System.out.println("Updated player: " + status);
        });
    }

    private void toggleReadyStatus() {
        try {
            ready = !ready;
            readyButton.setText(ready ? "Ready" : "Not Ready");
            readyButton.setStyle(ready ? "-fx-background-color: green;" : "-fx-background-color: red;");
            lobbySpace.put(playerName, ready ? "READY" : "NOT_READY");
            System.out.println(playerName + " is now " + (ready ? "ready" : "not ready"));
        } catch (InterruptedException e) {
            showError("Error toggling ready status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        System.out.println("Starting LobbyClient with SERVER_URI: " + SERVER_URI);
        launch(args);
    }
}
