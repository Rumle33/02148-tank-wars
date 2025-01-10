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

	public float maxVelocity = 200.0f; // Adjusted for smooth gameplay
	public float maxAngularVelocity = 90.0f; // Degrees per second

	public float friction = 0.95f; // Simulated friction (closer to 1 = less friction)
	public float angularFriction = 0.95f; // Simulated angular friction

	@Override
	public void update(float delta) {
		// Apply acceleration and clamp speed
		velocity = MathUtil.clamp(velocity + acceleration * delta, -maxVelocity, maxVelocity);
		angularVelocity = MathUtil.clamp(angularVelocity + angularAcceleration * delta, -maxAngularVelocity, maxAngularVelocity);

		// Apply friction to velocity and angular velocity
		velocity *= friction;
		angularVelocity *= angularFriction;

		// Update position and rotation
		x += Math.cos(Math.toRadians(rotation)) * velocity * delta;
		y += Math.sin(Math.toRadians(rotation)) * velocity * delta;
		rotation += angularVelocity * delta;

		// Reset acceleration after each update
		acceleration = 0.0f;
		angularAcceleration = 0.0f;

		System.out.println("Tank: x=" + x + ", y=" + y + ", rotation=" + rotation + ", velocity=" + velocity);
	}
}
