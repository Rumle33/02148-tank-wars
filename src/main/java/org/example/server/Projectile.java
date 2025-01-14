package org.example.server;

import org.example.util.Vector2f;

public class Projectile implements GameObject {
	public float x;
	public float y;
	public float rotation;
	public float velocity = 250.0f; // Adjust for visible speed
	public float ttl = 3.0f; // Time to live in seconds

	public Projectile(float x, float y, float rotation) {
		this.x = x;
		this.y = y;
		this.rotation = rotation;
	}

	@Override
	public boolean update(Simulation simulation, float delta) {
		this.ttl -= delta;
		if (this.ttl < 0) {
			return false;
		}

		Vector2f translate = new Vector2f(velocity, 0.0f).rotate(rotation);
		this.x = this.x + translate.x * delta;
		this.y = this.y + translate.y * delta;

		return true;
	}

	@Override
	public void serialize(StringBuilder buffer) {
		buffer.append("Projectile").append(" ")
				.append(this.x).append(" ")
				.append(this.y).append(" ")
				.append(this.rotation).append("\n");
	}

	public static final float RADIUS = 5.0f;

	@Override
	public float getAABBX() {
		return this.x - Projectile.RADIUS;
	}

	@Override
	public float getAABBY() {
		return this.y - Projectile.RADIUS;
	}

	@Override
	public float getAABBWidth() {
		return Projectile.RADIUS * 2;
	}

	@Override
	public float getAABBHeight() {
		return Projectile.RADIUS * 2;
	}

	@Override
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
