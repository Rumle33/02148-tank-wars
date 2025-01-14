package org.example.util;

import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;

public class Vector2f {
	
	public float x;
	public float y;

	public Vector2f() {
		this.x = 0;
		this.y = 0;
	}

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

	public Vector2f set(float x, float y) {
		this.x = x;
		this.y = y;

		return this;
	}

	public float dot() {
		return this.dot(this);
	}

	public float dot(Vector2f v) {
		return this.dot(v.x, v.y);
	}

	public float dot(float x, float y) {
		return this.x * x + this.y * y;	
	}

	public float length() {
		return (float)Math.sqrt(this.dot());
	}

	public Vector2f normalize() {
		float factor = 1.0f / (float)Math.sqrt(this.dot());

		this.x *= factor;
		this.y *= factor;

		return this;
	}

	public Vector2f apply(Matrix3f m) {
		float nx = m.m00 * this.x + m.m01 * this.y + m.m02;
		float ny = m.m10 * this.x + m.m11 * this.y + m.m12;

		this.x = nx;
		this.y = ny;

		return this;
	}

	public Vector2f from(Vector2f v) {
		this.x = v.x;
		this.y = v.y;

		return this;
	}

	public Vector2f from(float[] buffer, int offset) {
		this.x = buffer[offset];
		this.y = buffer[offset + 1];

		return this;
	}

	public void dump(float[] buffer, int offset) {
		buffer[offset] = this.x;
		buffer[offset + 1] = this.y;
	}

	public static void forEach(float[] data, Consumer<Vector2f> function) {
		Vector2f v = new Vector2f();
		for (int i = 0; i < data.length; i = i + 2) {
			function.accept(v.from(data, i));
		}
	}

	public static void apply(float[] input, float[] output, Function<Vector2f, Vector2f> function) {
		Vector2f v = new Vector2f();
		for (int i = 0; i < input.length; i = i + 2) {
			function.apply(v).dump(output, i);
		}
	}
}
