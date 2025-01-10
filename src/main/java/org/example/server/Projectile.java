package org.example.server;

public class Projectile implements GameObject {
	public float x;
	public float y;
	public float rotation;
	public float velocity = 400.0f; // Adjust for visible speed
	public float ttl = 3.0f; // Time to live in seconds

	public Projectile(float x, float y, float rotation) {
		this.x = x;
		this.y = y;
		this.rotation = rotation;
	}

	@Override
	public void update(float delta) {
		ttl -= delta;
		x += Math.cos(Math.toRadians(rotation)) * velocity * delta;
		y += Math.sin(Math.toRadians(rotation)) * velocity * delta;
	}

	public boolean isAlive() {
		return ttl > 0;
	}
}
