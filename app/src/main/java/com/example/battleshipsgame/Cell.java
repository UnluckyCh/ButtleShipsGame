// Cell.java
package com.example.battleshipsgame;

public class Cell {
    public int x;
    public int y;
    public boolean hasShip = false;
    public boolean isHit = false;
    public Ship ship = null; // Добавлено

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
