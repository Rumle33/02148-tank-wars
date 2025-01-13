package org.example.server;

import org.example.util.Vector2f;

public class PhysicsCollision {
	
	Vector2f s0 = new Vector2f();
	Vector2f s1 = new Vector2f();
	Vector2f s2 = new Vector2f();

	public PhysicsCollision() {

	}

	public boolean test(PhysicsComponent c0, PhysicsComponent c1) {
		Vector2f v = new Vector2f();

		Vector2f dir = new Vector2f(1, 0);

		{
			Vector2f t0 = c0.support(c0.dmesh, dir.x, dir.y);
			Vector2f t1 = c1.support(c1.dmesh, -dir.x, -dir.y);

			s0.set(t0.x - t1.x, t0.y - t1.y);
		}
		{
			dir.set(0 - s0.x, 0 - s0.y);

			Vector2f t0 = c0.support(c0.dmesh, dir.x, dir.y);
			Vector2f t1 = c1.support(c1.dmesh, -dir.x, -dir.y);

			s1.set(t0.x - t1.x, t0.y - t1.y);
		}

		// loop

		// project 0,0 onto line formed by s0 and s1
		// set direction to projected point to 0,0
		// check if 0,0 in triangle
		// if not, remove furthest point and repeat 
	

		return false;
	}

	// barycentric method
	private boolean originInTriangle(Vector2f v0, Vector2f v1, Vector2f v2) {
		float a = 1.0f / (v0.y * (v2.x - v1.x) + v0.x * (v1.y - v2.y) + v1.x * v2.y - v1.y * v2.x);
		float s = a * (v0.y * v2.x - v0.x * v2.y);
		float t = a * (v0.x * v1.y - v0.y * v1.x);
		
		if (s > 0 && t > 0 && 1 - s - t > 0) {
			return true;
		}

		return false;
	}

}
