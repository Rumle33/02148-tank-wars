package org.example.server;

import org.example.util.MathUtil;

public class Tank implements GameObject {

    public float x;
    public float y;
    public float rotation; // Rotation in radians
    public float velocity;
    public float angularVelocity;

    public float acceleration;
    public float angularAcceleration;

    public float maxVelocity = 50.0f; // Slower speed
    public float maxAngularVelocity = (float) Math.toRadians(30.0f); // Degrees per second
    public boolean isAlive = true;

    public final String name;
    public int score;

    private static final float AABB_WIDTH = 25.0f;  // Match sprite size
    private static final float AABB_HEIGHT = 25.0f; // Match sprite size

    public Tank(String name, int score) {
        this.name = name;
        this.score = score;
    }

    @Override
    public boolean update(Simulation simulation, float delta) {
        if (!isAlive) return false;

        // Gradual stop when no acceleration
        if (acceleration == 0) {
            velocity *= 0.9f; // Apply friction
            if (Math.abs(velocity) < 0.01f) velocity = 0;
        }
        if (angularAcceleration == 0) {
            angularVelocity *= 0.9f;
            if (Math.abs(angularVelocity) < 0.01f) angularVelocity = 0;
        }

        velocity = MathUtil.clamp(velocity + acceleration * delta, -maxVelocity, maxVelocity);
        angularVelocity = MathUtil.clamp(angularVelocity + angularAcceleration * delta, -maxAngularVelocity, maxAngularVelocity);

        rotation += angularVelocity * delta;

        float dx = (float) (velocity * Math.cos(rotation) * delta);
        float dy = (float) (velocity * Math.sin(rotation) * delta);

        x += dx;
        y += dy;

        if (simulation.handleTankWallCollision(this, dx, dy)) {
            velocity = 0;
            x -= dx;
            y -= dy;
        }

        return true;
    }

    public void kill(Simulation simulation, String message) {
        isAlive = false;
        System.out.println(name + ": " + message);
    }

    @Override
    public void serialize(StringBuilder buffer) {
        buffer.append(this.name).append(" ")
              .append(this.x).append(" ")
              .append(this.y).append(" ")
              .append(this.rotation).append(" ")
              .append(this.score).append(" ")
              .append(this.getAABBX()).append(" ")
              .append(this.getAABBY()).append(" ")
              .append(this.getAABBWidth()).append(" ")
              .append(this.getAABBHeight()).append(" ")
              .append("\n");
    }

    @Override
    public float getAABBX() {
        return this.x - (AABB_WIDTH / 2);
    }

    @Override
    public float getAABBY() {
        return this.y - (AABB_HEIGHT / 2);
    }

    @Override
    public float getAABBWidth() {
        return AABB_WIDTH;
    }

    @Override
    public float getAABBHeight() {
        return AABB_HEIGHT;
    }

    @Override
    public void collide(GameObject object) {
        if (object instanceof Projectile) {
            System.out.println("Tank hit by projectile!");
        }
    }
}
