package org.example.server;

import org.example.util.Vector2f;

public class Projectile implements GameObject {
	public float x;
	public float y;
	public float rotation;
	public float velocity = 400.0f;
	public float ttl = 3.0f;

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

		Vector2f translate = new Vector2f(1.0f, 0.0f).rotate(rotation);
		this.x = this.x + translate.x * delta;
		this.y = this.y + translate.y * delta;

		return true;
	}

	public boolean isAlive() {
		return this.ttl >= 0;
	}
}
