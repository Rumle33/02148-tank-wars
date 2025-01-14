package org.example.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.Tank.Main;
import javafx.geometry.Insets;

import java.io.*;
import java.net.*;

public class LobbyClient extends Application {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ListView<String> playerListView = new ListView<>();
    private boolean ready = false;
    private Button readyButton = new Button("Not Ready");
    private String playerName; // Local variable for player name

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
                playerName = name; // Set player name
                root.getChildren().clear(); // Clear initial UI
                root.getChildren().addAll(lobbyLabel, playerListView, readyButton);
                readyButton.setDisable(false); // Enable ready button
                connectToServer();
            } else {
                nameLabel.setText("Player name cannot be empty!");
            }
        });
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(playerName); // Send the player name to the server

            new Thread(this::listenForUpdates).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForUpdates() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received message: " + message); // Debugging line
                if (message.equals("START")) {
                    System.out.println("Game starting...");
                    Main.main(new String[]{}); // Launch the main game
                    break;
                } else if (message.startsWith("UPDATE")) {
                    updatePlayerList(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void updatePlayerList(String message) {
        System.out.println("Received update: " + message); // Debugging line
        String[] lines = message.split("\\n");
        Platform.runLater(() -> {
            playerListView.getItems().clear();
            for (int i = 1; i < lines.length; i++) { // Start from 1 to skip "UPDATE"
                playerListView.getItems().add(lines[i]);
            }
        });
    }


    private void toggleReadyStatus() {
        ready = !ready;
        readyButton.setText(ready ? "Ready" : "Not Ready");
        readyButton.setStyle(ready ? "-fx-background-color: green;" : "-fx-background-color: red;");
        out.println("READY");
    }

    public static void main(String[] args) {
        launch(args); // Launch the JavaFX application
    }
}
