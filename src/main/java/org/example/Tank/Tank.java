package org.example.Tank;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class Tank extends Application {

    private static final double WIDTH = 800;
    private static final double HEIGHT = 600;
    private static final double TANK_SIZE = 40;
    private static final double SPEED = 5;

    private Rectangle tank;

    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        tank = new Rectangle(TANK_SIZE, TANK_SIZE, Color.GREEN);
        tank.setX(WIDTH / 2 - TANK_SIZE / 2);
        tank.setY(HEIGHT / 2 - TANK_SIZE / 2);

        root.getChildren().add(tank);

        scene.setOnKeyPressed(this::handleKeyPress);

        primaryStage.setTitle("Tank Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case W:
                if (tank.getY() - SPEED >= 0) {
                    tank.setY(tank.getY() - SPEED);
                }
                break;
            case S:
                if (tank.getY() + TANK_SIZE + SPEED <= HEIGHT) {
                    tank.setY(tank.getY() + SPEED);
                }
                break;
            case A:
                if (tank.getX() - SPEED >= 0) {
                    tank.setX(tank.getX() - SPEED);
                }
                break;
            case D:
                if (tank.getX() + TANK_SIZE + SPEED <= WIDTH) {
                    tank.setX(tank.getX() + SPEED);
                }
                break;
            case SPACE:
                Projectile.shoot();
                break;

        }
    }
}