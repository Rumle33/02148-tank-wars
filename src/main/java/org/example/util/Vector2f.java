package org.example.util;

public class Vector2f {
	
	public float x;
	public float y;

	public Vector2f(Vector2f v) {
		this.x = v.x;
		this.y = v.y;
	}

	public Vector2f(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vector2f rotate(float angle) {

		float nx = this.x * (float)Math.cos(angle) - this.y * (float)Math.sin(angle);
		float ny = this.x * (float)Math.sin(angle) + this.y * (float)Math.cos(angle);

		this.x = nx;
		this.y = ny;

		return this;
	}

}
