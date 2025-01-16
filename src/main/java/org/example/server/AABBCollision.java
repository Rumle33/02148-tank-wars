package org.example.server;

import org.example.Maps.Wall;
import org.example.util.AABB;

public class AABBCollision {

	public static boolean test(GameObject obj, Wall wall) {
		float objX0 = obj.getAABBX();
		float objY0 = obj.getAABBY();
		float objX1 = objX0 + obj.getAABBWidth();
		float objY1 = objY0 + obj.getAABBHeight();

		float wallX0 = (float) Math.min(wall.getStartX(), wall.getEndX());
		float wallY0 = (float) Math.min(wall.getStartY(), wall.getEndY());
		float wallX1 = (float) Math.max(wall.getStartX(), wall.getEndX());
		float wallY1 = (float) Math.max(wall.getStartY(), wall.getEndY());

		// Return true if there's any overlap between the AABB and the wall
		return objX1 > wallX0 && objX0 < wallX1 && objY1 > wallY0 && objY0 < wallY1;
	}



}
