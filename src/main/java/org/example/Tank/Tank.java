package org.example.Tank;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.example.Maps.Wall;
import org.example.server.Projectile;
import org.jspace.Space;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tank extends Application {

	private boolean showDebug = true;
    private Space lobbySpace;
    private Space gameSpace;
    private String playerName;
    private final HashMap<String, ImageView> tanks = new HashMap<>();
    private Pane root;
    private final Set<String> keysPressed = new HashSet<>();
    private long lastShotTime = 0;
    private List<Circle> projectiles = new ArrayList<>();
    private List<Rectangle> quads = new ArrayList<>();
    private List<Wall> mapWalls;
	private List<Polygon> polygons = new ArrayList<>();
    private Scene scene;

    @Override
    public void start(Stage primaryStage) {
        try {
            if (lobbySpace == null || gameSpace == null || root == null) {
                throw new IllegalStateException("Spaces and root must be initialized before starting the game.");
            }


            Scene scene = new Scene(root, 800, 600);
            this.scene = scene;

            primaryStage.setScene(scene);
            primaryStage.setTitle("Tank Game");
            primaryStage.show();


            new Thread(this::joinLobby).start();
            setupKeyHandling(scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void joinLobby() {
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

    public void setupKeyHandling(Scene scene) {
        scene.setOnKeyPressed(event -> {
            keysPressed.add(event.getCode().toString());
            if (event.getCode().toString().equals("SPACE")) {
                shootProjectile();
            }
            sendActionsToServer();
        });

        scene.setOnKeyReleased(event -> {
			if (event.getCode() == KeyCode.H) {
				this.showDebug = !this.showDebug;
			}
			keysPressed.remove(event.getCode().toString());
            sendActionsToServer();
        });
    }

    private void shootProjectile() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime >= 100) { // 1-second cooldown
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
		for (Polygon polygon : this.polygons) {
			root.getChildren().remove(polygon);
		}
        this.projectiles.clear();
		this.quads.clear();
		this.polygons.clear();

        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts[0].startsWith("Player")) {
                String playerName = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double rotation = Double.parseDouble(parts[3]);

				int score = Integer.parseInt(parts[4]);

				float ax0 = Float.parseFloat(parts[5]);
				float ay0 = Float.parseFloat(parts[6]);
				float awidth = Float.parseFloat(parts[7]);
				float aheight = Float.parseFloat(parts[8]);

				double[] mesh = new double[org.example.server.Tank.MESH.length];
				for (int i = 0; i < mesh.length; i++) {
					mesh[i] = Float.parseFloat(parts[9 + i]);
				}

				if (this.showDebug)
				{
					// draw collision mesh
					Polygon polygon = new Polygon(mesh);
					polygon.setFill(null);
					polygon.setStroke(Color.ORANGE);
					this.polygons.add(polygon);
					root.getChildren().add(polygon);

					// draw AABB
					Rectangle quad = new Rectangle(ax0, ay0, awidth, aheight);
					quad.setFill(null);
					quad.setStroke(Color.RED);
					quad.setStrokeWidth(1); 
					this.quads.add(quad);
					root.getChildren().add(quad);
				}

				// draw tank
				ImageView tank = tanks.computeIfAbsent(playerName, playerNameKey -> {
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

				tank.setX(x - 12.5);
				tank.setY(y - 12.5);
				tank.setRotate(Math.toDegrees(rotation));
                
            } else if (parts[0].equals("Projectile")) {
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);

				float ax0 = Float.parseFloat(parts[4]);
				float ay0 = Float.parseFloat(parts[5]);
				float awidth = Float.parseFloat(parts[6]);
				float aheight = Float.parseFloat(parts[7]);

				double[] mesh = new double[Projectile.MESH.length];
				for (int i = 0; i < mesh.length; i++) {
					mesh[i] = Float.parseFloat(parts[8 + i]);
				}

				if (this.showDebug)
				{
					// draw collision mesh
					Polygon polygon = new Polygon(mesh);
					polygon.setFill(null);
					polygon.setStroke(Color.ORANGE);
					this.polygons.add(polygon);
					root.getChildren().add(polygon);

					// draw AABB
					Rectangle quad = new Rectangle(ax0, ay0, awidth, aheight);
					quad.setFill(null);
					quad.setStroke(Color.RED);
					quad.setStrokeWidth(1); 
					this.quads.add(quad);
					root.getChildren().add(quad); 
				}

				// draw projectile
                Circle projectile = new Circle(Projectile.PROJECTILE_RADIUS, Color.PURPLE);
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

    public void setRoot(Pane root) {
        this.root = root;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public void setLobbySpace(Space lobbySpace) {
        this.lobbySpace = lobbySpace;
    }

    public void setGameSpace(Space gameSpace) {
        this.gameSpace = gameSpace;
    }
}