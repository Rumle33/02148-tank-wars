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

	public float maxVelocity = 100.0f; // Pixels per second
	public float maxAngularVelocity = (float) Math.toRadians(30.0f); // Degrees per second
	public boolean isAlive = true;


	public final String name;

	public int score;

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
			if (Math.abs(velocity) < 0.01f) velocity = 0; // Snap to zero if very slow
		}
		if (angularAcceleration == 0) {
			angularVelocity *= 0.9f;
			if (Math.abs(angularVelocity) < 0.01f) angularVelocity = 0; // Snap to zero
		}

		// Update velocity
		velocity += acceleration * delta;
		angularVelocity += angularAcceleration * delta;

		// Allow free rotation
		rotation += angularVelocity * delta;

		// Calculate movement
		float dx = (float) (velocity * Math.cos(rotation) * delta);
		float dy = (float) (velocity * Math.sin(rotation) * delta);

		// Apply movement
		x += dx;
		y += dy;

		// Check for collisions
		if (simulation.handleTankWallCollision(this, dx, dy)) {
			// Undo position updates if there was a collision
			x -= dx;
			y -= dy;
		}

		return true;
	}




	@Override
	public void serialize(StringBuilder buffer) {
		buffer
			.append(this.name).append(" ")
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


	private final float AABB_WIDTH = 10.0f;  // Red square width (matches the tank's size)
	private final float AABB_HEIGHT = 10.0f; // Red square height (matches the tank's size)

	@Override
	public float getAABBX() {
		return this.x - (AABB_WIDTH / 2); // Center the AABB horizontally
	}

	@Override
	public float getAABBY() {
		return this.y - (AABB_HEIGHT / 2); // Center the AABB vertically
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
			System.out.println("Tank hit projectile!");
			this.isAlive = false;
		} else if (object instanceof Tank) {
			System.out.println("Tank hit tank!");
		} else {
			System.out.println("Tank hit unknown!");
		}
	}
}
