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

	public float maxVelocity = 1000.0f; // Pixels per second
	public float maxAngularVelocity = 90.0f; // Degrees per second
	public boolean isAlive = true;

	public void update(float delta) {
		if (!isAlive) return;

		// Gradual stop when no acceleration
		if (acceleration == 0) {
			velocity *= 0.95f; // Apply friction
			if (Math.abs(velocity) < 0.01f) velocity = 0; // Snap to zero
		}
		if (angularAcceleration == 0) {
			angularVelocity *= 0.95f;
			if (Math.abs(angularVelocity) < 0.01f) angularVelocity = 0;
		}

		// Update velocity and position
		velocity = MathUtil.clamp(velocity + acceleration * delta, -maxVelocity, maxVelocity);
		angularVelocity = MathUtil.clamp(angularVelocity + angularAcceleration * delta, -maxAngularVelocity, maxAngularVelocity);
		rotation += angularVelocity * delta;

		float dx = (float) (velocity * Math.cos(Math.toRadians(rotation)) * delta);
		float dy = (float) (velocity * Math.sin(Math.toRadians(rotation)) * delta);
		x += dx;
		y += dy;

		// Reset acceleration after update
		acceleration = 0;
		angularAcceleration = 0;
	}
}
