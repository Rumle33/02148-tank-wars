package org.example.Maps;

public class Wall {
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

    public boolean matches(double startX, double startY, double endX, double endY) {
        return this.startX == startX && this.startY == startY && this.endX == endX && this.endY == endY;
    }
}
