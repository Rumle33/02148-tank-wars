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
import java.util.Arrays;

public class LobbyClient extends Application {
    // URI for connecting to the lobby server
    private static final String SERVER_URI = "tcp://localhost:9001/lobby?keep";

    // Space for interacting with the server
    private RemoteSpace lobbySpace;

    // GUI components for the lobby
    private final ListView<String> playerListView = new ListView<>();
    private boolean ready = false; // Tracks if the player is ready
    private final Button readyButton = new Button("Not Ready"); // Button to toggle readiness
    private String playerName; // Stores the player's name
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // Background thread for listening to updates

    @Override
    public void start(Stage primaryStage) {
        // Create the main layout
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Input for player's name and join button
        Label nameLabel = new Label("Enter your player name:");
        TextField nameField = new TextField();
        Button joinButton = new Button("Join Lobby");

        // Label and button for the lobby
        Label lobbyLabel = new Label("Lobby - Connected Players");
        readyButton.setStyle("-fx-background-color: red;"); // Default button color for "Not Ready"
        readyButton.setOnAction(e -> toggleReadyStatus()); // Action for toggling readiness
        readyButton.setDisable(true); // Disabled until the player joins the lobby

        // Add components for name entry to the layout
        root.getChildren().addAll(nameLabel, nameField, joinButton);

        // Create and set the main scene
        Scene scene = new Scene(root, 300, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Tank Game Lobby");
        primaryStage.show();

        // Action for the join button
        joinButton.setOnAction(e -> {
            String name = nameField.getText().trim(); // Get the entered name
            if (!name.isEmpty()) {
                playerName = name; // Save the player's name
                root.getChildren().clear(); // Clear the current UI
                root.getChildren().addAll(lobbyLabel, playerListView, readyButton); // Add lobby UI components
                readyButton.setDisable(false); // Enable the ready button
                connectToServer(); // Connect to the server
            } else {
                System.err.println("Player name cannot be empty!"); // Error for empty name
            }
        });
    }

    // Connect to the server and join the lobby
    private void connectToServer() {
        try {
            System.out.println("Attempting to connect to server at " + SERVER_URI);
            lobbySpace = new RemoteSpace(SERVER_URI); // Connect to the lobby server
            System.out.println("Connection established. Joining lobby...");
            lobbySpace.put(playerName, "JOIN"); // Send a "JOIN" action to the server
            System.out.println("Connected to server as " + playerName);
            listenForUpdates(); // Start listening for updates from the server
        } catch (Exception e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Listen for updates from the server in a background thread
    private void listenForUpdates() {
        executor.submit(() -> {
            try {
                while (true) {
                    // Wait for an update from the server
                    Object[] update = lobbySpace.get(new FormalField(String.class), new FormalField(String.class));
                    String type = (String) update[0]; // Type of the update (e.g., "UPDATE", "START_GAME")
                    String message = (String) update[1]; // The actual message

                    if (type.equals("UPDATE")) {
                        // Update the player list in the lobby
                        updatePlayerList(message);
                    } else if (type.equals("START_GAME") && message.equals("ALL")) {
                        // Handle the start of the game
                        Platform.runLater(() -> {
                            System.out.println("All players are ready. Starting the game!");
                            // Placeholder: Add logic to handle game start
                        });
                        break;
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("Error in listenForUpdates: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // Update the player list in the UI
    private void updatePlayerList(String message) {
        Platform.runLater(() -> {
            synchronized (playerListView) {
                playerListView.getItems().clear(); // Clear the current list
                if (!message.isEmpty()) {
                    String[] players = message.split(","); // Split the player list
                    playerListView.getItems().addAll(players); // Add the updated player list
                    System.out.println("Player list updated: " + Arrays.toString(players));
                } else {
                    System.out.println("Player list is empty."); // Log if the list is empty
                }
            }
        });
    }

    // Toggle the player's readiness state
    private void toggleReadyStatus() {
        try {
            ready = !ready; // Toggle readiness
            readyButton.setText(ready ? "Ready" : "Not Ready"); // Update button text
            readyButton.setStyle(ready ? "-fx-background-color: green;" : "-fx-background-color: red;"); // Update button color
            lobbySpace.put(playerName, ready ? "READY" : "NOT_READY"); // Send readiness state to the server
            System.out.println(playerName + " is now " + (ready ? "ready" : "not ready"));
        } catch (InterruptedException e) {
            System.err.println("Error toggling ready status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args); // Launch the JavaFX application
    }
}
