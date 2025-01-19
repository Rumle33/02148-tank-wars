package org.example.server;

import org.example.Maps.Wall;
import org.example.util.MathUtil;

public class Tank implements GameObject {

	public static final float TANK_WIDTH = 25.0f;
	public static final float TANK_HEIGHT = 25.0f;

	private float x;
	private float y;
	private float rotation; // Rotation in radians
	private float velocity;
	private float angularVelocity;

	public float acceleration;
	public float angularAcceleration;

	public float maxVelocity = 50.0f; // Pixels per second
	public float maxAngularVelocity = (float) Math.toRadians(30.0f); // Degrees per second
	public boolean isAlive = true;

	public final String name;

	public int score;

	private PhysicsComponent physics;

	public static final float MESH[] = {
		-TANK_HEIGHT / 2, -TANK_WIDTH / 2,
		-TANK_HEIGHT / 2, TANK_WIDTH / 2,
		TANK_HEIGHT / 2, TANK_WIDTH / 2,
		TANK_HEIGHT / 2, -TANK_WIDTH / 2
	};

	public Tank(String name, int score) {
		this.name = name;
		this.score = score;

		this.physics = new PhysicsComponent(0, 0, 0, Tank.MESH);
	}

	@Override
	public boolean update(Simulation simulation, float delta) {
		if (!isAlive) return false;

		this.x = physics.x;
		this.y = physics.y;
		this.rotation = physics.r;

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

		float dr = angularVelocity * delta;
		float dx = (float) (velocity * Math.cos(rotation) * delta);
		float dy = (float) (velocity * Math.sin(rotation) * delta);

		this.physics.dx = dx;
		this.physics.dy = dy;
		this.physics.dr = dr;

		// rotation += dr;
		// x += dx;
		// y += dy;

		// Update AABB
		final float width = Tank.TANK_WIDTH;
		final float height = Tank.TANK_HEIGHT;

		float dfr = this.rotation + physics.dr;
		float dfx = this.x + physics.dx;
		float dfy = this.y + physics.dy;

		this.aabb_width = (float)(Math.abs(Math.cos(dfr)) * height + Math.abs(Math.sin(dfr)) * width);
		this.aabb_height = (float)(Math.abs(Math.sin(dfr)) * height + Math.abs(Math.cos(dfr)) * width);
		this.aabb_x = dfx - this.aabb_width * 0.5f;
		this.aabb_y = dfy - this.aabb_height * 0.5f;

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
		;

		float[] mesh = this.physics.getTransformMesh();
		for (int i = 0; i < mesh.length; i++) {
			buffer.append(mesh[i]).append(" ");
		}

		buffer.append("\n");
	}

	private float aabb_x, aabb_y;
	private float aabb_width, aabb_height;

	public float getX() {
		return this.x;
	}

	public float getY() {
		return this.y;
	}

	public float getRotation() {
		return this.rotation;
	}

	public Tank setX(float x) {
		this.x = x;
		this.physics.x = x;

		return this;
	}

	public Tank setY(float y) {
		this.y = y;
		this.physics.y = y;

		return this;
	}

	public Tank setRotation(float rotation) {
		this.rotation = rotation;
		this.physics.r = rotation;

		return this;
	}

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

	@Override
	public void collide(Simulation simulation, Object object) {
		this.acceleration = 0;
		this.angularAcceleration = 0;

		if (object instanceof Projectile) {
			System.out.println("Tank hit projectile!");
			this.isAlive = false;

			simulation.showMessage(this.name + " looses!");
		} else if (object instanceof Tank) {
			System.out.println("Tank hit tank!");
		} else if (object instanceof Wall) {
			System.out.println("Tank hit wall!");
		} else {
			System.out.println("Tank hit unknown!");
		}
	}

	@Override
	public PhysicsComponent getPhysicsComponent() {
		return this.physics;
	}
}
