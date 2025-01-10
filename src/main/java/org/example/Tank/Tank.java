package org.example.Tank;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.jspace.RemoteSpace;
import org.jspace.Space;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Tank extends Application {

    private Space lobbySpace;
    private Space gameSpace;
    private String playerName;
    private final Map<String, Group> tanks = new HashMap<>();
    private Pane root;

    private final Set<String> keysPressed = new HashSet<>();
    private long lastShotTime = 0;

	private List<Circle> projectiles = new ArrayList<>();

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

		for (Circle circle : this.projectiles) {
			root.getChildren().remove(circle);
		}
		this.projectiles.clear();

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

    public static void main(String[] args) {
        launch(args);
    }
}
