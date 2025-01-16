package org.example.Maps;

import org.example.util.AABB;

public class Wall implements AABB {
    final double startX, startY, endX, endY;

    public Wall(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public double getEndX() {
        return endX;
    }

    public double getEndY() {
        return endY;
    }

    @Override
    public float getAABBX() {
        return (float) Math.min(startX, endX);
    }

    @Override
    public float getAABBY() {
        return (float) Math.min(startY, endY);
    }

    @Override
    public float getAABBWidth() {
        return (float) Math.abs(endX - startX);
    }

    @Override
    public float getAABBHeight() {
        return (float) Math.abs(endY - startY);
    }
}
