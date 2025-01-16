package org.example.server;

import org.example.Maps.Wall;

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

        // Return true if there's any overlapo between the AABB and the wall
        return objX1 > wallX0 && objX0 < wallX1 && objY1 > wallY0 && objY0 < wallY1;
    }

    public static boolean test(GameObject obj1, GameObject obj2) {
        float obj1X0 = obj1.getAABBX();
        float obj1Y0 = obj1.getAABBY();
        float obj1X1 = obj1X0 + obj1.getAABBWidth();
        float obj1Y1 = obj1Y0 + obj1.getAABBHeight();

        float obj2X0 = obj2.getAABBX();
        float obj2Y0 = obj2.getAABBY();
        float obj2X1 = obj2X0 + obj2.getAABBWidth();
        float obj2Y1 = obj2Y0 + obj2.getAABBHeight();

        // Return true if there's anyy overlap between the AABBs
        return obj1X1 > obj2X0 && obj1X0 < obj2X1 && obj1Y1 > obj2Y0 && obj1Y0 < obj2Y1;
    }
}
