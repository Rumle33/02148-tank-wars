package org.example.server;

import org.example.util.Vector2f;

public class PhysicsCollision {

	public static boolean test(PhysicsComponent c0, PhysicsComponent c1) {
		float[] c0d = c0.getDeltaMesh();
		float[] c1d = c1.getDeltaMesh();

		Vector2f v0 = new Vector2f();
		Vector2f v1 = new Vector2f();
		Vector2f v2 = new Vector2f();
		Vector2f line = new Vector2f();

		for (int i = 0; i < c0d.length; i = i + 2) {
			v0.from(c0d, i);
			v1.from(c0d, (i + 2) % c0d.length);
			line.set(v0.y - v1.y, v1.x - v0.x).normalize();

			float c0t0 = Float.MAX_VALUE;
			float c0t1 = Float.MIN_VALUE;

			float c1t0 = Float.MAX_VALUE;
			float c1t1 = Float.MIN_VALUE;

			// test c0d
			for (int j = 0; j < c0d.length; j = j + 2) {
				float t = v2.from(c0d, j).dot(line);
			
				if (t < c0t0) {
					c0t0 = t;
				}
				if (t > c0t1) {
					c0t1 = t;
				}
			}

			// test c1d
			for (int j = 0; j < c0d.length; j = j + 2) {
				float t = v2.from(c1d, j).dot(line);

				if (t < c1t0) {
					c1t0 = t;
				}
				if (t > c1t1) {
					c1t1 = t;
				}
			}

			if (c0t0 > c1t1 || c1t0 > c0t1) {
				return false;
			}

		}

		for (int i = 0; i < c1d.length; i = i + 2) {
			v0.from(c1d, i);
			v1.from(c1d, (i + 2) % c1d.length);
			line.set(v1.x - v0.x, v1.y - v0.y).normalize();
			line.set(-line.y, line.x);

			float c0t0 = Float.MAX_VALUE;
			float c0t1 = Float.MIN_VALUE;

			float c1t0 = Float.MAX_VALUE;
			float c1t1 = Float.MIN_VALUE;

			// test c0d
			for (int j = 0; j < c0d.length; j = j + 2) {
				float t = v2.from(c0d, j).dot(line);
			
				if (t < c0t0) {
					c0t0 = t;
				}
				if (t > c0t1) {
					c0t1 = t;
				}
			}

			// test c1d
			for (int j = 0; j < c0d.length; j = j + 2) {
				float t = v2.from(c1d, j).dot(line);

				if (t < c1t0) {
					c1t0 = t;
				}
				if (t > c1t1) {
					c1t1 = t;
				}
			}

			if (c0t0 >= c1t1 || c1t0 >= c0t1) {
				return false;
			}

		}

		c0.dx = 0;
		c0.dy = 0;
		c0.dr = 0;

		c1.dx = 0;
		c1.dy = 0;
		c1.dr = 0;

		return true;
	}

}
