package org.example.util;

public class MathUtil {
	
	public static Vector2f rotate2f(Vector2f val, float angle) {

		return new Vector2f(
			val.x * (float)Math.cos(angle) - val.y * (float)Math.sin(angle), 
			val.x * (float)Math.sin(angle) + val.y * (float)Math.cos(angle)
		);

	}

	public static float clamp(float val, float min, float max) {
		return val < min ? min : val > max ? max : val;
	}

}
