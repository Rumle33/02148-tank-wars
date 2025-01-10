package org.example.Tank;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.jspace.RemoteSpace;
import org.jspace.Space;

import java.util.HashMap;
import java.util.Map;

public class Tank extends Application {

    private Space lobbySpace;
    private Space gameSpace;
    private String playerName;
    private final Map<String, Group> tanks = new HashMap<>();
    private Pane root;

    @Override
    public void start(Stage primaryStage) {
        try {
            lobbySpace = new RemoteSpace("tcp://localhost:12345/lobby?keep");
            gameSpace = new RemoteSpace("tcp://localhost:12345/game?keep");

            root = new Pane();
            Scene scene = new Scene(root, 800, 600);

            primaryStage.setScene(scene);
            primaryStage.setTitle("Tank Game");
            primaryStage.show();

            new Thread(this::joinLobby).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void joinLobby() {
        try {
            playerName = "Player" + (int) (Math.random() * 1000);
            lobbySpace.put("JOIN", playerName);
            lobbySpace.get(new org.jspace.ActualField("START_GAME"), new org.jspace.ActualField(playerName));

            javafx.application.Platform.runLater(this::startGame);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startGame() {
        root.getScene().setOnKeyPressed(event -> {
        try {
            switch (event.getCode()) {
                case W -> gameSpace.put("ACTION", playerName, "MOVE", 1.0f); // Forward
                case S -> gameSpace.put("ACTION", playerName, "MOVE", -1.0f); // Backward
                case A -> gameSpace.put("ACTION", playerName, "ROTATE", -1.0f); // Rotate Left
                case D -> gameSpace.put("ACTION", playerName, "ROTATE", 1.0f); // Rotate Right
                case SPACE -> gameSpace.put("ACTION", playerName, "SHOOT", 0.0f); // Shoot
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });

        root.getScene().setOnKeyReleased(event -> {
            try {
                switch (event.getCode()) {
                    case W, S -> gameSpace.put("ACTION", playerName, "MOVE", 0.0f); // Stop movement
                    case A, D -> gameSpace.put("ACTION", playerName, "ROTATE", 0.0f); // Stop rotation
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        startGameLoop();
    }

    private void startGameLoop() {
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateGameState();
            }
        };
        gameLoop.start();
    }

    private void updateGameState() {
        try {
            Object[] state = gameSpace.get(new org.jspace.ActualField("STATE"), new org.jspace.FormalField(String.class));
            if (state != null) {
                renderGameState((String) state[1]);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void renderGameState(String gameState) {
        String[] lines = gameState.split("\n");
        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts[0].startsWith("Player")) {
                String playerName = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double rotation = Double.parseDouble(parts[3]);

                javafx.application.Platform.runLater(() -> {
                    Group tank = tanks.computeIfAbsent(playerName, name -> {
                        Group newTank = new Group(new Rectangle(40, 40, name.equals(this.playerName) ? Color.GREEN : Color.BLUE));
                        root.getChildren().add(newTank);
                        return newTank;
                    });
                    tank.setTranslateX(x - 20);
                    tank.setTranslateY(y - 20);
                    tank.setRotate(rotation);
                });
            } else if (parts[0].equals("Projectile")) {
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);

                javafx.application.Platform.runLater(() -> {
                    Rectangle projectile = new Rectangle(10, 10, Color.RED);
                    projectile.setTranslateX(x - 5);
                    projectile.setTranslateY(y - 5);
                    root.getChildren().add(projectile);

                    // Schedule to remove projectile after a short time
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000); // Match the TTL of the projectile
                            javafx.application.Platform.runLater(() -> root.getChildren().remove(projectile));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
            }
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
