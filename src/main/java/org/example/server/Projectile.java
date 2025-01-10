package org.example.server;

import org.example.util.Vector2f;

public class Projectile implements GameObject {
	public float x;
	public float y;
	public float rotation;
	public float velocity = 100.0f; // Adjust for visible speed
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

	public boolean isAlive() {
		return this.ttl >= 0;
	}
}
