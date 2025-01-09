package org.example.Tank;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Tank extends Application {

    private static final double WIDTH = 800;
    private static final double HEIGHT = 600;
    private static final double TANK_SIZE = 40;
    private static final double BARREL_WIDTH = 10;
    private static final double BARREL_HEIGHT = 30;
    private static final double SPEED = 5;
    private static final double ROTATION_SPEED = 5;

    private Group tankGroup; // Group containing the tank body and barrel
    private Rectangle tankBody;
    private Rectangle tankBarrel;
    private double tankRotation = 0; // Tank rotation in degrees
    private Pane root;
    private List<Projectile> projectiles;

    @Override
    public void start(Stage primaryStage) {
        root = new Pane();
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        // Tank body
        tankBody = new Rectangle(TANK_SIZE, TANK_SIZE, Color.GREEN);

        // Tank barrel
        tankBarrel = new Rectangle(BARREL_WIDTH, BARREL_HEIGHT, Color.DARKGRAY);
        tankBarrel.setTranslateX(TANK_SIZE / 2 - BARREL_WIDTH / 2); // Center barrel horizontally
        tankBarrel.setTranslateY(-BARREL_HEIGHT); // Extend barrel upward from the tank body

        // Group the tank body and barrel
        tankGroup = new Group(tankBody, tankBarrel);
        tankGroup.setTranslateX(WIDTH / 2 - TANK_SIZE / 2); // Initial position
        tankGroup.setTranslateY(HEIGHT / 2 - TANK_SIZE / 2);

        projectiles = new ArrayList<>();
        root.getChildren().add(tankGroup);

        scene.setOnKeyPressed(this::handleKeyPress);

        primaryStage.setTitle("Tank Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        startGameLoop();
    }

    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case W: // Move forward
                moveTank(SPEED);
                break;
            case S: // Move backward
                moveTank(-SPEED);
                break;
            case A: // Rotate left
                rotateTank(-ROTATION_SPEED);
                break;
            case D: // Rotate right
                rotateTank(ROTATION_SPEED);
                break;
            case SPACE: // Shoot
                shoot();
                break;
        }
    }

    private void moveTank(double distance) {
        // Calculate movement based on the current tank rotation
        double dx = distance * Math.cos(Math.toRadians(tankRotation));
        double dy = distance * Math.sin(Math.toRadians(tankRotation));

        // Ensure the tank stays within bounds
        if (tankGroup.getTranslateX() + dx >= 0 && tankGroup.getTranslateX() + dx + TANK_SIZE <= WIDTH) {
            tankGroup.setTranslateX(tankGroup.getTranslateX() + dx);
        }
        if (tankGroup.getTranslateY() + dy >= 0 && tankGroup.getTranslateY() + dy + TANK_SIZE <= HEIGHT) {
            tankGroup.setTranslateY(tankGroup.getTranslateY() + dy);
        }
    }

    private void rotateTank(double angle) {
        // Update the tank's rotation
        tankRotation += angle;
        tankGroup.setRotate(tankRotation);
    }

    private void shoot() {
        // Calculate the tip of the barrel based on the current tank rotation
        double barrelTipX = tankGroup.getTranslateX() + TANK_SIZE / 2 +
                (BARREL_HEIGHT * Math.cos(Math.toRadians(tankRotation)));
        double barrelTipY = tankGroup.getTranslateY() + TANK_SIZE / 2 +
                (BARREL_HEIGHT * Math.sin(Math.toRadians(tankRotation)));

        // Create and add a projectile
        Projectile projectile = new Projectile(barrelTipX, barrelTipY, tankRotation);
        projectiles.add(projectile);
        root.getChildren().add(projectile.getBullet());
    }

    private void startGameLoop() {
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateProjectiles();
            }
        };
        gameLoop.start();
    }

    private void updateProjectiles() {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.move();

            // Remove projectiles that go off-screen
            if (projectile.getBullet().getX() < 0 || projectile.getBullet().getX() > WIDTH ||
                    projectile.getBullet().getY() < 0 || projectile.getBullet().getY() > HEIGHT) {
                root.getChildren().remove(projectile.getBullet());
                iterator.remove();
            }
        }
    }
}
