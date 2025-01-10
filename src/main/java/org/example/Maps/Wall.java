package org.example.Maps;

public class Wall {
    final double startX, startY, endX, endY;

    public Wall(double startX, double startY,double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public boolean matches(double startX, double startY,double endX, double endY) {
        return this.startX == startX && this.startY == startY && this.endX == endX && this.endY == endY;
    }
}