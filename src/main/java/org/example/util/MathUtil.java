package org.example.util;

public class MathUtil {
	
	public static Vector2f rotate2f(Vector2f val, float angle) {
		return new Vector2f(val).rotate(angle);
	}

	public static float clamp(float val, float min, float max) {
		return val < min ? min : val > max ? max : val;
	}

	public Vector2f collideMM(float[] vertexData0, float[] vertexData1) {
		for (int i = 0; i < vertexData0.length - 2; i = i + 2) {
			// get axis to test
			float axisX = vertexData0[i + 1] - vertexData0[i + 2 + 1];
			float axisY = vertexData0[i + 2] - vertexData0[i];

			// normalize axis
			{
				float nfactor = 1.0f / (float) Math.sqrt(axisX * axisX + axisY * axisY);
				axisX *= nfactor;
				axisY *= nfactor;
			}

			// project first mesh
			


			// project second mesh

		}

		return null;
	}

	public Vector2f collideMC(float[] vertexData0, float x, float y, float radius) {
		for (int i = 0; i < vertexData0.length >> 1; i++) {

		}

		return null;
	}

}
