package org.example.util;

public class Matrix3f {
	
	float m00, m01, m02;
	float m10, m11, m12;
	float m20, m21, m22;

	public Matrix3f() {
		this.setIdentity();
	}

	public Matrix3f(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
	}

	public Matrix3f(Vector3f r0, Vector3f r1, Vector3f r2) {
		this(
			r0.x, r0.y, r0.z, 
			r1.x, r1.y, r1.z, 
			r2.x, r2.y, r2.z
		);
	}

	public Matrix3f setIdentity() {
		this.m00 = 1;
		this.m01 = 0;
		this.m02 = 0;
		this.m10 = 0;
		this.m11 = 1;
		this.m12 = 0;
		this.m20 = 0;
		this.m21 = 0;
		this.m22 = 1;
		return this;
	}

	public Matrix3f setRotate(float angle) {
		this.m00 = (float)Math.cos(angle);
		this.m01 = -(float)Math.sin(angle);
		this.m10 = (float)Math.sin(angle);
		this.m11 = (float)Math.cos(angle);

		return this;
	}

	public Matrix3f setTranslate(float x, float y) {
		this.m02 = x;
		this.m12 = y;
		return this;
	}
	
	// for applying transforms onto a 2d point
	public Vector2f applyTo(float x, float y) {
		return new Vector2f(
			this.m00 * x + this.m01 * y + this.m02,
			this.m10 * x + this.m11 * y + this.m12
		);
	}

	public Vector2f applyTo(Vector2f v) {
		return this.applyTo(v.x, v.y);
	}

	public Vector3f multiply(Vector3f v) {
		return new Vector3f(
			this.m00 * v.x + this.m01 * v.y + this.m02 * v.z,
			this.m10 * v.x + this.m11 * v.y + this.m12 * v.z,
			this.m20 * v.x + this.m21 * v.y + this.m22 * v.z
		);
	}

}
