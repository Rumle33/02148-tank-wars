package org.example.util;

public class Vector3f {
	
	public float x;
	public float y;
	public float z;

	public Vector3f() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}

	public Vector3f(Vector3f v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	public Vector3f(Vector2f v, float z) {
		this.x = v.x;
		this.y = v.y;
		this.z = z;
	}

	public Vector3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

}
