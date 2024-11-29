// Ship.java
package com.example.battleshipsgame;

import java.util.ArrayList;
import java.util.List;

public class Ship {
    public List<Cell> cells;
    public boolean isMine = false; // Добавлено

    public Ship() {
        cells = new ArrayList<>();
    }

    public boolean isSunk() {
        for (Cell cell : cells) {
            if (!cell.isHit) {
                return false;
            }
        }
        return true;
    }
}
