package org.example.server;

import org.example.util.AABB;

public class AABBCollision {
	
	public static boolean test(AABB c0, AABB c1) {
		float c0x0 = c0.getAABBX();
		float c0y0 = c0.getAABBY();
		float c0x1 = c0x0 + c0.getAABBWidth();
		float c0y1 = c0y0 + c0.getAABBHeight();

		float c1x0 = c1.getAABBX();
		float c1y0 = c1.getAABBY();
		float c1x1 = c1x0 + c1.getAABBWidth();
		float c1y1 = c1y0 + c1.getAABBHeight();

		if ((c1x0 > c0x0 && c1x0 < c0x1) || (c1x1 > c0x0 && c1x1 < c0x1)) {
			if ((c1y0 > c0y0 && c1y0 < c0y1) || (c1y1 > c0y0 && c1y1 < c0y0)) {
				return true;
			}
		}

		return false;
	}
}
