package org.example.server;

public class Projectile implements GameObject {
	
	public float x; 
	public float y;
	public float rotation;
	public float velocity;

	public float ttl;

	@Override
	public void update(float delta) {
		this.ttl -= delta;
		if (this.ttl < 0) {
			// die
		}
	}

	
}
