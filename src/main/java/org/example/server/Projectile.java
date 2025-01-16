package org.example.server;

import org.example.Maps.Wall;
import org.example.util.Vector2f;

public class Projectile implements GameObject {
    public float x;
    public float y;
    public float rotation;
    public float velocity = 250.0f; 
    public float ttl = 3.0f; // Time to live in seconds
    private final Tank shooter; // Reference to the shooter
    private final Simulation simulation;

    public Projectile(float x, float y, float rotation, Tank shooter, Simulation simulation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.shooter = shooter;
        this.simulation = simulation;
    }

    @Override
    public boolean update(Simulation simulation, float delta) {
        ttl -= delta;
        if (ttl < 0) {
            return false; 
        }

        // Move the projectile
        x += Math.cos(rotation) * velocity * delta;
        y += Math.sin(rotation) * velocity * delta;

        // Check for collisions with tanks
        for (Tank tank : simulation.getTanks()) {
            if (tank != shooter && AABBCollision.test(this, tank)) {
                handleTankCollision(tank);
                return false; 
            }
        }

    
        for (Wall wall : simulation.getMapWalls()) {
            if (AABBCollision.test(this, wall)) {
                // Bounce logic
                handleWallCollision(wall);
                return true; 
            }
        }

        return true; // Projectile remains active
    }

    private void handleTankCollision(Tank tank) {
        System.out.println("Projectile hit tank!");


        simulation.showMessage(tank.name + " loses!");
        simulation.showMessage(shooter.name + " wins!");


        tank.isAlive = false;

       
        this.ttl = -1;
    }

    private void handleWallCollision(Wall wall) {
        boolean isHorizontal = Math.abs(wall.getEndY() - wall.getStartY()) < Math.abs(wall.getEndX() - wall.getStartX());

        if (isHorizontal) {
            rotation = (float) (-rotation); // Reverse Y direction
        } else {
            rotation = (float) (Math.PI - rotation); // Reverse X direction
        }

        x += Math.cos(rotation) * 2.0f;
        y += Math.sin(rotation) * 2.0f;
    }

    @Override
    public void serialize(StringBuilder buffer) {
        buffer.append("Projectile ")
              .append(this.x).append(" ")
              .append(this.y).append(" ")
              .append(this.rotation).append("\n");
    }

    @Override
    public float getAABBX() {
        return this.x - 5.0f; // Center AABB
    }

    @Override
    public float getAABBY() {
        return this.y - 5.0f; // Center AABB
    }

    @Override
    public float getAABBWidth() {
        return 10.0f; // AABB width
    }

    @Override
    public float getAABBHeight() {
        return 10.0f; // AABB height
    }

	public void collide(GameObject object) {

		if (object instanceof Tank) {
			System.out.println("Projectile hit tank!");
			this.ttl = -1;
		}
		else if (object instanceof Projectile) {
			System.out.println("Projectile hit projectile!");
			this.ttl = -1;
		}
		else {
			System.out.println("Projectile hit unknown!");
		}

	}
}
