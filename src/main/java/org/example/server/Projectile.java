package org.example.server;

import org.example.util.Vector2f;

public class Projectile implements GameObject {

	public static final float PROJECTILE_RADIUS = 5.0f;

	public float x;
	public float y;
	public float rotation;
	public float velocity = 250.0f; // Adjust for visible speed
	public float ttl = 3.0f; // Time to live in seconds

	private PhysicsComponent physics;

	static {
		int points = 4;
		Projectile.MESH = new float[points * 2];

		for (int i = 0; i < points; i++) {
			float angle = (float)i / (float)points * (float)Math.PI * 2.0f;
			Projectile.MESH[i * 2 + 0] = PROJECTILE_RADIUS * (float) Math.cos(angle);
			Projectile.MESH[i * 2 + 1] = PROJECTILE_RADIUS * (float) Math.sin(angle);
		}
	}

	public static float MESH[];

	public Projectile(float x, float y, float rotation) {
		this.x = x;
		this.y = y;
		this.rotation = rotation;

		this.physics = new PhysicsComponent(x, y, rotation, MESH);
	}

	@Override
	public boolean update(Simulation simulation, float delta) {
		this.ttl -= delta;
		if (this.ttl < 0) {
			return false;
		}

		this.x = physics.x;
		this.y = physics.y;
		this.rotation = physics.r;

		Vector2f translate = new Vector2f(velocity, 0.0f).rotate(rotation);

		float dx = translate.x * delta;
		float dy = translate.y * delta; 

		this.physics.dx = dx;
		this.physics.dy = dy;

		// this.x = this.x + dx;
		// this.y = this.y + dy;

		return true;
	}

	@Override
	public void serialize(StringBuilder buffer) {
		buffer
			.append("Projectile").append(" ")
			.append(this.x).append(" ")
			.append(this.y).append(" ")
			.append(this.rotation).append(" ")
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

	@Override
	public float getAABBX() {
		return this.x - Projectile.PROJECTILE_RADIUS;
	}

	@Override
	public float getAABBY() {
		return this.y - Projectile.PROJECTILE_RADIUS;
	}

	@Override
	public float getAABBWidth() {
		return Projectile.PROJECTILE_RADIUS * 2;
	}

	@Override
	public float getAABBHeight() {
		return Projectile.PROJECTILE_RADIUS * 2;
	}

	@Override
	public void collide(GameObject object) {

		if (object instanceof Tank) {
			System.out.println("Projectile hit tank!");
			// this.ttl = -1;
		}
		else if (object instanceof Projectile) {
			System.out.println("Projectile hit projectile!");
			this.ttl = -1;
		}
		else {
			System.out.println("Projectile hit unknown!");
		}

	}

	@Override
	public PhysicsComponent getPhysicsComponent() {
		return this.physics;
	}
}
