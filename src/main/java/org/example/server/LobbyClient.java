package org.example.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.jspace.*;
import javafx.geometry.Insets;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class LobbyClient extends Application {
    private static final String SERVER_URI = "tcp://localhost:9001/lobby?keep";
    private RemoteSpace lobbySpace;
    private ListView<String> playerListView = new ListView<>();
    private boolean ready = false;
    private Button readyButton = new Button("Not Ready");
    private String playerName;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Input for player name
        Label nameLabel = new Label("Enter your player name:");
        TextField nameField = new TextField();
        Button joinButton = new Button("Join Lobby");

        // Lobby UI components
        Label lobbyLabel = new Label("Lobby - Connected Players");
        readyButton.setStyle("-fx-background-color: red;");
        readyButton.setOnAction(e -> toggleReadyStatus());
        readyButton.setDisable(true); // Disable ready button until player joins

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
                nameLabel.setText("Player name cannot be empty!");
            }
        });
    }

    private void connectToServer() {
        try {
            lobbySpace = new RemoteSpace(SERVER_URI);
            lobbySpace.put(playerName, "JOIN");
            listenForUpdates();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenForUpdates() {
        executor.submit(() -> {
            try {
                while (true) {
                    Object[] update = lobbySpace.get(new FormalField(String.class), new FormalField(String.class));
                    String type = (String) update[0];
                    String message = (String) update[1];

                    if (type.equals("UPDATE")) {
                        System.out.println("Received player list update: " + message); // Debug
                        updatePlayerList(message);
                    } else if (type.equals("START_GAME") && message.equals("ALL")) {
                        Platform.runLater(() -> {
                            System.out.println("Game starting...");
                            // Transition to game scene
                        });
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void updatePlayerList(String message) {
        Platform.runLater(() -> {
            playerListView.getItems().clear();
            if (!message.isEmpty()) {
                String[] players = message.split(",");
                playerListView.getItems().addAll(players);
            }
        });
    }

    private void toggleReadyStatus() {
        try {
            ready = !ready;
            readyButton.setText(ready ? "Ready" : "Not Ready");
            readyButton.setStyle(ready ? "-fx-background-color: green;" : "-fx-background-color: red;");
            lobbySpace.put(playerName, ready ? "READY" : "NOT_READY");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}