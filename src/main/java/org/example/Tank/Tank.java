package org.example.Tank;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.jspace.RemoteSpace;
import org.jspace.Space;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tank extends Application {

    private Space lobbySpace;
    private Space gameSpace;
    private String playerName;
    private final HashMap<String, ImageView> tanks = new HashMap<>();
    private Pane root;
    private final Set<String> keysPressed = new HashSet<>();
    private long lastShotTime = 0;
    private List<Circle> projectiles = new ArrayList<>();
    private List<Rectangle> quads = new ArrayList<>();

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
            setupKeyHandling(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void joinLobby() {
        try {
            playerName = "Player" + (int) (Math.random() * 1000);
            lobbySpace.put("JOIN", playerName);
            lobbySpace.get(new org.jspace.ActualField("START_GAME"), new org.jspace.ActualField(playerName));

            gameSpace.put("REQUEST_MAP", playerName);
            Object[] mapData = gameSpace.get(new org.jspace.ActualField("MAP"), new org.jspace.FormalField(String.class));
            renderMap((String) mapData[1]);

            javafx.application.Platform.runLater(this::startGame);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setupKeyHandling(Scene scene) {
        scene.setOnKeyPressed(event -> {
            keysPressed.add(event.getCode().toString());
            if (event.getCode().toString().equals("SPACE")) {
                shootProjectile();
            }
            sendActionsToServer();
        });

        scene.setOnKeyReleased(event -> {
            keysPressed.remove(event.getCode().toString());
            sendActionsToServer();
        });
    }

    private void shootProjectile() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime >= 1000) { // 1-second cooldown
            lastShotTime = currentTime;

            try {
                gameSpace.put("ACTION", playerName, "SHOOT", 0.0f);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendActionsToServer() {
        float move = 0.0f;
        float rotate = 0.0f;

        if (keysPressed.contains("W")) {
            move += 3.0f;
        }
        if (keysPressed.contains("S")) {
            move -= 3.0f;
        }
        if (keysPressed.contains("A")) {
            rotate -= 3.0f;
        }
        if (keysPressed.contains("D")) {
            rotate += 3.0f;
        }

        try {
            gameSpace.put("ACTION", playerName, "MOVE", move);
            gameSpace.put("ACTION", playerName, "ROTATE", rotate);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startGame() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateGameState();
            }
        }.start();
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

        for (Circle circle : this.projectiles) {
            root.getChildren().remove(circle);
        }
        for (Rectangle rectangle : this.quads) {
            root.getChildren().remove(rectangle);
        }
        this.projectiles.clear();
        this.quads.clear();

        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts[0].startsWith("Player")) {
                String playerName = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double rotation = Double.parseDouble(parts[3]);

                float ax0 = Float.parseFloat(parts[5]);
                float ay0 = Float.parseFloat(parts[6]);
                float awidth = Float.parseFloat(parts[7]);
                float aheight = Float.parseFloat(parts[8]);

                Rectangle quad = new Rectangle(ax0, ay0, awidth, aheight);
                quad.setFill(null);
                quad.setStroke(Color.RED);
                quad.setStrokeWidth(1);
                this.quads.add(quad);
                root.getChildren().add(quad);

                javafx.application.Platform.runLater(() -> {
                    ImageView tank = tanks.computeIfAbsent(playerName, key -> {
                        ImageView newTank = new ImageView(new Image(
                                playerName.equals(this.playerName)
                                        ? getClass().getResource("/assets/BlueTank.png").toExternalForm()
                                        : getClass().getResource("/assets/RedTank.png").toExternalForm()
                        ));
                        newTank.setFitWidth(25);
                        newTank.setFitHeight(25);
                        root.getChildren().add(newTank);
                        return newTank;
                    });

                    tank.setX(x - 20);
                    tank.setY(y - 20);
                    tank.setRotate(Math.toDegrees(rotation));
                });
            } else if (parts[0].equals("Projectile")) {
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);

                Circle projectile = new Circle(5, Color.RED);
                projectile.setTranslateX(x);
                projectile.setTranslateY(y);
                root.getChildren().add(projectile);
                this.projectiles.add(projectile);
            }
        }
    }

    private void renderMap(String mapData) {
        javafx.application.Platform.runLater(() -> {
            String[] lines = mapData.split("\n");
            for (String line : lines) {
                String[] parts = line.split(" ");
                if (parts.length != 4) {
                    continue;
                }

                double startX = Double.parseDouble(parts[0]);
                double startY = Double.parseDouble(parts[1]);
                double endX = Double.parseDouble(parts[2]);
                double endY = Double.parseDouble(parts[3]);

                Line wall = new Line(startX, startY, endX, endY);
                wall.setStroke(Color.BLACK);
                wall.setStrokeWidth(2);
                root.getChildren().add(wall);
            }
            System.out.println("Map rendered successfully.");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}