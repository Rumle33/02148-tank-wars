package org.example.Tank;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Projectile {
    private static final double SIZE = 10; // Size of the projectile
    private static final double SPEED = 10; // Speed of the projectile

    private Rectangle bullet;
    private double direction; // Direction in degrees

    public Projectile(double x, double y, double direction) {
        this.direction = direction;

        // Create a graphical representation of the projectile
        bullet = new Rectangle(SIZE, SIZE, Color.RED);
        bullet.setX(x - SIZE / 2); // Center the bullet
        bullet.setY(y - SIZE / 2);
    }

    public Rectangle getBullet() {
        return bullet;
    }

    public void move() {
        double dx = SPEED * Math.cos(Math.toRadians(direction));
        double dy = SPEED * Math.sin(Math.toRadians(direction));
        bullet.setX(bullet.getX() + dx);
        bullet.setY(bullet.getY() + dy);
    }
}
