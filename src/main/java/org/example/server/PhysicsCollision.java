package org.example.server;

import org.example.util.Vector2f;

public class PhysicsCollision {
	
	Vector2f s0 = new Vector2f();
	Vector2f s1 = new Vector2f();
	Vector2f s2 = new Vector2f();

	public PhysicsCollision() {

	}

	public boolean test(PhysicsComponent c0, PhysicsComponent c1) {
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
	
		Vector2f line = new Vector2f();
		Vector2f proj = new Vector2f();

		for (;;) {
			// do a bit of a cheat and shift to 0,0 and then back
			line.set(s1.x - s0.x, s1.y - s0.y);
			float t = 1.0f / line.length();
			proj.set(s0.x - line.x * t, s0.y - line.y * t);

			Vector2f t0 = c0.support(c0.dmesh, proj.x, proj.y);
			Vector2f t1 = c1.support(c1.dmesh, -proj.x, -proj.y);

			s2.set(t0.x - t1.x, t0.y - t1.y);

			if (originInTriangle(this.s0, this.s1, this.s2)) {
				return true;
			}

			float s0l = s0.dot();
			float s1l = s1.dot();
			float s2l = s2.dot();

			// remove furthest and check if done
			if (s2l >= s0l) {
				// s2l >= s0l
				if (s2l >= s1l) {
					// s2l >= s1l
					// nothing left
					break;
				}
				// s2l >= s0l
				// s2l < s1l
				// s0l <= s2l < s1l
				s1.from(s2);
			}
			else if (s2l >= s1l) {
				// s2l < s0l
				// s2l >= s1l
				// s1l < s2l < s0l
				s0.from(s2);
			} else {
				// s2l < s0l
				// s2l < s1l
				if (s0l >= s1l) {
					// s2l < s1l <= s0l
					s0.from(s2);
				} else {
					// s2l < s0l < s1l
					s1.from(s2);
				}
			}
		}


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
