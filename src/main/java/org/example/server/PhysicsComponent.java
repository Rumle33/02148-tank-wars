package org.example.server;

import org.example.util.Matrix3f;
import org.example.util.Vector2f;

public class PhysicsComponent {
	
	private float[] meshData;
	public float x, y, r;
	public float dx, dy, dr;

	// translate matrix
	private Matrix3f tm = new Matrix3f();
	// delta matrix
	private Matrix3f dm = null;

	private float[] tmesh;
	private float[] dmesh;

	public PhysicsComponent(float x, float y, float r, float[] meshData) {
		this.x = x;
		this.y = y;
		this.r = r;
		this.meshData = meshData;
		this.dx = 0;
		this.dy = 0;
		this.dr = 0;

		this.tm.setTranslate(this.x, this.y).setRotate(this.r);

		this.tmesh = new float[this.meshData.length];
		this.dmesh = new float[this.meshData.length];

		this.updateTMesh();
	}

	public void update() {
		this.x += this.dx;
		this.y += this.dy;
		this.r += this.dr;

		this.dx = 0;
		this.dy = 0;
		this.dr = 0;

		this.tm.setTranslate(this.x, this.y).setRotate(this.r);
		this.dm = null;

		this.updateTMesh();
	}

	private void updateTMesh() {
		Matrix3f m = this.getMeshTransform();
		Vector2f v = new Vector2f();

		for (int i = 0; i < this.meshData.length; i = i + 2) {
			v.from(this.meshData, i).apply(m).dump(tmesh, i);
		}
	}

	public float[] getTransformMesh() {
		return this.tmesh;
	}

	private void updateDMesh() {
		Matrix3f m = this.getDeltaTransform();
		Vector2f v = new Vector2f();

		for (int i = 0; i < this.meshData.length; i = i + 2) {
			v.from(this.meshData, i).apply(m).dump(dmesh, i);
		}
	}

	public float[] getDeltaMesh() {
		this.getDeltaTransform();
		if (this.dm == null) {
			this.updateDMesh();
		}
		return this.dmesh;
	}

	public Matrix3f getMeshTransform() {
		return this.tm;
	}

	public Matrix3f getDeltaTransform() {
		if (this.dm == null) {
			this.dm = new Matrix3f()
			.setTranslate(this.x + this.dx, this.y + this.dy)
			.setRotate(this.r + this.dr);
			this.updateDMesh();
		}
		return this.dm;
	}
}
