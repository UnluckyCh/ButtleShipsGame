package com.example.battleshipsgame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board {
    public Cell[][] cells;
    public List<Ship> ships;

    public Board() {
        cells = new Cell[10][10];
        ships = new ArrayList<>();
        initializeCells();
    }

    private void initializeCells() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                cells[i][j] = new Cell(i, j);
            }
        }
    }

    public void placeShips() {
        placeShip(4); // Один четырёхпалубный корабль
        for (int i = 0; i < 2; i++) {
            placeShip(3); // Два трёхпалубных корабля
        }
        for (int i = 0; i < 3; i++) {
            placeShip(2); // Три двухпалубных корабля
        }
        for (int i = 0; i < 2; i++) {
            placeShip(1); // Два однопалубных корабля
        }
    }

    private void placeShip(int size) {
        Random random = new Random();
        boolean placed = false;

        while (!placed) {
            int x = random.nextInt(10);
            int y = random.nextInt(10);
            boolean horizontal = random.nextBoolean();

            if (canPlaceShip(x, y, size, horizontal)) {
                Ship ship = new Ship();
                for (int i = 0; i < size; i++) {
                    int dx = horizontal ? x : x + i;
                    int dy = horizontal ? y + i : y;
                    Cell cell = cells[dx][dy];
                    cell.hasShip = true;
                    ship.cells.add(cell);
                }
                ships.add(ship);
                placed = true;
            }
        }
    }

    private boolean canPlaceShip(int x, int y, int size, boolean horizontal) {
        for (int i = 0; i < size; i++) {
            int dx = horizontal ? x : x + i;
            int dy = horizontal ? y + i : y;

            if (dx >= 10 || dy >= 10 || cells[dx][dy].hasShip) {
                return false;
            }

            // Проверка на расстояние от других кораблей
            for (int adjX = dx - 1; adjX <= dx + 1; adjX++) {
                for (int adjY = dy - 1; adjY <= dy + 1; adjY++) {
                    if (adjX >= 0 && adjX < 10 && adjY >= 0 && adjY < 10) {
                        if (cells[adjX][adjY].hasShip) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
