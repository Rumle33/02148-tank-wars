package org.example.server;

import org.example.util.MathUtil;

public class Tank implements GameObject {
	
	public float x;
	public float y;
	public float rotation;
	public float velocity;
	public float angularVelocity;

	public float acceleration;
	public float angularAcceleration;

	public float maxVelocity = 100.0f; // Pixels per second
	public float maxAngularVelocity = (float) Math.toRadians(30.0f); // Degrees per second
	public boolean isAlive = true;

	public final String name;

	public Tank(String name) {
		this.name = name;
	}

	@Override
	public boolean update(Simulation simulation, float delta) {
		if (!isAlive) return false;

		// Gradual stop when no acceleration
		if (acceleration == 0) {
			velocity *= 0.9f; // Apply friction
			if (Math.abs(velocity) < 0.01f) velocity = 0; // Snap to zero
		}
		if (angularAcceleration == 0) {
			angularVelocity *= 0.9f;
			if (Math.abs(angularVelocity) < 0.01f) angularVelocity = 0;
		}

		// Update velocity and position
		velocity = MathUtil.clamp(velocity + acceleration * delta, -maxVelocity, maxVelocity);
		angularVelocity = MathUtil.clamp(angularVelocity + angularAcceleration * delta, -maxAngularVelocity, maxAngularVelocity);
		rotation += angularVelocity * delta;

		float dx = (float) (velocity * Math.cos(rotation) * delta);
		float dy = (float) (velocity * Math.sin(rotation) * delta);
		x += dx;
		y += dy;

		// update AABB

		final float width = 40.0f;
		final float height = 40.0f;

		this.aabb_width = (float)(Math.cos(this.rotation) * width + Math.sin(this.rotation) * height);
		this.aabb_height = (float)(Math.sin(this.rotation) * width + Math.sin(this.rotation) * height);
		this.aabb_x = this.x - this.aabb_width * 0.5f;
		this.aabb_y = this.y - this.aabb_height * 0.5f;

		return true;
	}

	@Override
	public void serialize(StringBuilder buffer) {
		buffer.append(this.name).append(" ")
				.append(this.x).append(" ")
				.append(this.y).append(" ")
				.append(this.rotation).append("\n");
	}

	public float aabb_x, aabb_y;
	public float aabb_width, aabb_height;

	@Override
	public float getAABBX() {
		return this.aabb_x;
	}

	@Override
	public float getAABBY() {
		return this.aabb_y;
	}

	@Override
	public float getAABBWidth() {
		return this.aabb_width;
	}

	@Override
	public float getAABBHeight() {
		return this.aabb_height;
	}
}
