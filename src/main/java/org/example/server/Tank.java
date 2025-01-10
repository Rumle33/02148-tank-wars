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

<<<<<<< HEAD
	public float maxVelocity = 200.0f; // Adjusted for smooth gameplay
	public float maxAngularVelocity = 90.0f; // Degrees per second

	public float friction = 0.95f; // Simulated friction (closer to 1 = less friction)
	public float angularFriction = 0.95f; // Simulated angular friction

	@Override
	public void update(float delta) {
		// Apply acceleration and clamp speed
=======
	public float maxVelocity = 1000.0f; // Pixels per second
	public float maxAngularVelocity = (float) Math.toRadians(90.0f); // Degrees per second
	public boolean isAlive = true;

	public boolean update(Simulation simulation, float delta) {
		if (!isAlive) return false;

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
>>>>>>> 3f6e8d194f954ade3f8268f253e30855588a11f7
		velocity = MathUtil.clamp(velocity + acceleration * delta, -maxVelocity, maxVelocity);
		angularVelocity = MathUtil.clamp(angularVelocity + angularAcceleration * delta, -maxAngularVelocity, maxAngularVelocity);

		// Apply friction to velocity and angular velocity
		velocity *= friction;
		angularVelocity *= angularFriction;

		// Update position and rotation
		x += Math.cos(Math.toRadians(rotation)) * velocity * delta;
		y += Math.sin(Math.toRadians(rotation)) * velocity * delta;
		rotation += angularVelocity * delta;

<<<<<<< HEAD
		// Reset acceleration after each update
		acceleration = 0.0f;
		angularAcceleration = 0.0f;

		System.out.println("Tank: x=" + x + ", y=" + y + ", rotation=" + rotation + ", velocity=" + velocity);
=======
		float dx = (float) (velocity * Math.cos(rotation) * delta);
		float dy = (float) (velocity * Math.sin(rotation) * delta);
		x += dx;
		y += dy;

		// Reset acceleration after update
		acceleration = 0;
		angularAcceleration = 0;

		return true;
>>>>>>> 3f6e8d194f954ade3f8268f253e30855588a11f7
	}
}
