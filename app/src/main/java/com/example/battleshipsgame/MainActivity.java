package com.example.battleshipsgame;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private GridLayout opponentGrid;
    private GridLayout playerGrid;
    private GridLayout shipSelectionGrid;
    private Button rotateButton;
    private Button startGameButton;
    private TextView shipSelectionLabel;
    private static final int GRID_SIZE = 10;

    private enum Turn {
        PLAYER,
        COMPUTER
    }

    private enum Orientation {
        UNKNOWN,
        HORIZONTAL,
        VERTICAL
    }

    private Orientation currentOrientation = Orientation.UNKNOWN;

    private Turn currentTurn = Turn.PLAYER;
    private Orientation shipPlacementOrientation = Orientation.HORIZONTAL;
    private boolean playerMustSkip = false;
    private boolean computerMustSkip = false;
    private boolean gameStarted = false;
    private Random random = new Random();
    private Handler handler = new Handler();
    private Point lastHitPoint = null;
    private List<Point> potentialTargets = new ArrayList<>();

    private Board playerBoard;
    private Board opponentBoard;
    private List<Point> playerMines = new ArrayList<>();
    private List<Point> opponentMines = new ArrayList<>();

    private ShipType selectedShipType = null;
    private Map<ShipType, Integer> shipQuantities = new EnumMap<>(ShipType.class);

    private enum ShipType {
        FOUR_DECK,
        THREE_DECK,
        TWO_DECK,
        ONE_DECK,
        MINE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        opponentGrid = findViewById(R.id.opponentGrid);
        playerGrid = findViewById(R.id.playerGrid);
        shipSelectionGrid = findViewById(R.id.shipSelectionGrid);
        rotateButton = findViewById(R.id.rotateButton);
        startGameButton = findViewById(R.id.startGameButton);
        shipSelectionLabel = findViewById(R.id.shipSelectionLabel);

        // Кнопка справки
        ImageButton helpButton = findViewById(R.id.helpButton);
        helpButton.setOnClickListener(v -> showHelpDialog());

        // Инициализация кораблей и мин для размещения
        initializeShipQuantities();
        setupShipSelectionButtons();

        rotateButton.setOnClickListener(v -> toggleShipPlacementOrientation());
        rotateButton.setText("Повернуть (Горизонтально)");

        startGameButton.setOnClickListener(v -> startGame());
        startGameButton.setVisibility(View.GONE);

        startNewGame();
    }

    private void initializeShipQuantities() {
        shipQuantities.put(ShipType.FOUR_DECK, 1);
        shipQuantities.put(ShipType.THREE_DECK, 2);
        shipQuantities.put(ShipType.TWO_DECK, 3);
        shipQuantities.put(ShipType.ONE_DECK, 2);
        shipQuantities.put(ShipType.MINE, 2);

        updateAllShipQuantityDisplays();
    }

    private void updateAllShipQuantityDisplays() {
        for (ShipType shipType : ShipType.values()) {
            updateShipQuantityDisplay(shipType);
        }
    }

    private void setupShipSelectionButtons() {
        findViewById(R.id.ship_four_deck).setOnClickListener(v -> selectShip(ShipType.FOUR_DECK, v));
        findViewById(R.id.ship_three_deck).setOnClickListener(v -> selectShip(ShipType.THREE_DECK, v));
        findViewById(R.id.ship_two_deck).setOnClickListener(v -> selectShip(ShipType.TWO_DECK, v));
        findViewById(R.id.ship_one_deck).setOnClickListener(v -> selectShip(ShipType.ONE_DECK, v));
        findViewById(R.id.ship_mine).setOnClickListener(v -> selectShip(ShipType.MINE, v));
    }

    private void selectShip(ShipType shipType, View view) {
        if (shipQuantities.get(shipType) > 0) {
            selectedShipType = shipType;
            resetShipSelectionButtons();
            view.setBackgroundColor(Color.LTGRAY);
        } else {
            Toast.makeText(this, "Нет доступных объектов этого типа.", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetShipSelectionButtons() {
        findViewById(R.id.ship_four_deck).setBackgroundColor(Color.WHITE);
        findViewById(R.id.ship_three_deck).setBackgroundColor(Color.WHITE);
        findViewById(R.id.ship_two_deck).setBackgroundColor(Color.WHITE);
        findViewById(R.id.ship_one_deck).setBackgroundColor(Color.WHITE);
        findViewById(R.id.ship_mine).setBackgroundColor(Color.WHITE);
    }

    private void toggleShipPlacementOrientation() {
        if (shipPlacementOrientation == Orientation.HORIZONTAL) {
            shipPlacementOrientation = Orientation.VERTICAL;
            rotateButton.setText("Повернуть (Вертикально)");
        } else {
            shipPlacementOrientation = Orientation.HORIZONTAL;
            rotateButton.setText("Повернуть (Горизонтально)");
        }
    }

    private void showHelpDialog() {
        // Создаём диалог с разметкой справки
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Справка");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_help, null);
        builder.setView(dialogView);

        builder.setPositiveButton("Закрыть", null);
        builder.show();
    }

    private void startNewGame() {
        currentTurn = Turn.PLAYER;
        playerMustSkip = false;
        computerMustSkip = false;
        playerMines.clear();
        opponentMines.clear();
        gameStarted = false;
        lastHitPoint = null;
        potentialTargets.clear();
        currentOrientation = Orientation.UNKNOWN;

        playerBoard = new Board();
        opponentBoard = new Board();

        opponentGrid.removeAllViews();
        playerGrid.removeAllViews();

        createGridCells(playerGrid, false);

        opponentGrid.setVisibility(View.GONE);
        findViewById(R.id.opponentLabel).setVisibility(View.GONE);

        shipSelectionGrid.setVisibility(View.VISIBLE);
        rotateButton.setVisibility(View.VISIBLE);
        shipSelectionLabel.setVisibility(View.VISIBLE);

        startGameButton.setVisibility(View.GONE);

        initializeShipQuantities();
        resetShipSelectionButtons();
        enableAllShipButtons();
    }

    private void createGridCells(GridLayout gridLayout, boolean isOpponentGrid) {
        Board board = isOpponentGrid ? opponentBoard : playerBoard;

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                Button cellButton = new Button(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.columnSpec = GridLayout.spec(j, 1f);
                params.rowSpec = GridLayout.spec(i, 1f);
                params.setMargins(2, 2, 2, 2);
                cellButton.setLayoutParams(params);

                cellButton.setBackgroundColor(Color.WHITE);

                final int x = i;
                final int y = j;

                if (!isOpponentGrid) {
                    cellButton.setOnClickListener(v -> {
                        if (selectedShipType != null && !gameStarted) {
                            handleShipPlacement(x, y);
                        } else if (!gameStarted) {
                            Cell cell = playerBoard.cells[x][y];
                            if (cell.ship != null) {
                                removeShipFromBoard(cell.ship);
                                updatePlayerGrid();
                            }
                        }
                    });
                } else {
                    cellButton.setOnClickListener(v -> {
                        if (currentTurn == Turn.PLAYER && gameStarted && cellButton.isEnabled()) {
                            handlePlayerMove(cellButton, x, y);
                        }
                    });
                }

                gridLayout.addView(cellButton);
            }
        }
    }

    private void handleShipPlacement(int x, int y) {
        if (selectedShipType == null) return;

        int shipSize = getShipSize(selectedShipType);

        if (canPlaceShip(playerBoard, x, y, shipSize, shipPlacementOrientation)) {
            placeShipOnBoard(playerBoard, x, y, shipSize, shipPlacementOrientation, selectedShipType == ShipType.MINE);

            int quantity = shipQuantities.get(selectedShipType) - 1;
            shipQuantities.put(selectedShipType, quantity);
            updateShipQuantityDisplay(selectedShipType);

            if (quantity == 0) {
                disableShipButton(selectedShipType);
            }

            selectedShipType = null;
            resetShipSelectionButtons();
            updatePlayerGrid();

            if (areAllShipsPlaced()) {
                startGameButton.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Все объекты размещены! Нажмите 'В бой' для начала игры.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Невозможно разместить объект здесь.", Toast.LENGTH_SHORT).show();
        }
    }

    private int getShipSize(ShipType shipType) {
        switch (shipType) {
            case FOUR_DECK:
                return 4;
            case THREE_DECK:
                return 3;
            case TWO_DECK:
                return 2;
            case ONE_DECK:
            case MINE:
                return 1;
            default:
                return 0;
        }
    }

    private boolean canPlaceShip(Board board, int x, int y, int size, Orientation orientation) {
        for (int i = 0; i < size; i++) {
            int dx = orientation == Orientation.HORIZONTAL ? x : x + i;
            int dy = orientation == Orientation.HORIZONTAL ? y + i : y;

            if (dx >= GRID_SIZE || dy >= GRID_SIZE || board.cells[dx][dy].hasShip || board.cells[dx][dy].ship != null) {
                return false;
            }

            for (int adjX = dx - 1; adjX <= dx + 1; adjX++) {
                for (int adjY = dy - 1; adjY <= dy + 1; adjY++) {
                    if (adjX >= 0 && adjX < GRID_SIZE && adjY >= 0 && adjY < GRID_SIZE) {
                        if (board.cells[adjX][adjY].hasShip || board.cells[adjX][adjY].ship != null) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void placeShipOnBoard(Board board, int x, int y, int size, Orientation orientation, boolean isMine) {
        Ship ship = new Ship();
        ship.isMine = isMine;

        for (int i = 0; i < size; i++) {
            int dx = orientation == Orientation.HORIZONTAL ? x : x + i;
            int dy = orientation == Orientation.HORIZONTAL ? y + i : y;
            Cell cell = board.cells[dx][dy];
            cell.hasShip = !isMine;
            cell.ship = ship;
            ship.cells.add(cell);

            if (isMine) {
                playerMines.add(new Point(dx, dy));
            }
        }

        if (!isMine) {
            board.ships.add(ship);
        }
    }

    private void removeShipFromBoard(Ship ship) {
        if (ship.isMine) {
            for (Cell cell : ship.cells) {
                cell.hasShip = false;
                cell.ship = null;
                playerMines.remove(new Point(cell.x, cell.y));
            }

            int quantity = shipQuantities.get(ShipType.MINE) + 1;
            shipQuantities.put(ShipType.MINE, quantity);
            updateShipQuantityDisplay(ShipType.MINE);
            enableShipButton(ShipType.MINE);
        } else {
            for (Cell cell : ship.cells) {
                cell.hasShip = false;
                cell.ship = null;
            }
            playerBoard.ships.remove(ship);

            int size = ship.cells.size();
            ShipType shipType = getShipTypeBySize(size);
            int quantity = shipQuantities.get(shipType) + 1;
            shipQuantities.put(shipType, quantity);
            updateShipQuantityDisplay(shipType);
            enableShipButton(shipType);
        }
    }

    private ShipType getShipTypeBySize(int size) {
        switch (size) {
            case 4:
                return ShipType.FOUR_DECK;
            case 3:
                return ShipType.THREE_DECK;
            case 2:
                return ShipType.TWO_DECK;
            case 1:
                return ShipType.ONE_DECK;
            default:
                return null;
        }
    }

    private void updateShipQuantityDisplay(ShipType shipType) {
        TextView quantityTextView = null;
        int quantity = shipQuantities.get(shipType);
        switch (shipType) {
            case FOUR_DECK:
                quantityTextView = findViewById(R.id.ship_four_deck_quantity);
                break;
            case THREE_DECK:
                quantityTextView = findViewById(R.id.ship_three_deck_quantity);
                break;
            case TWO_DECK:
                quantityTextView = findViewById(R.id.ship_two_deck_quantity);
                break;
            case ONE_DECK:
                quantityTextView = findViewById(R.id.ship_one_deck_quantity);
                break;
            case MINE:
                quantityTextView = findViewById(R.id.ship_mine_quantity);
                break;
        }
        if (quantityTextView != null) {
            quantityTextView.setText(String.valueOf(quantity));
        }
    }

    private void disableShipButton(ShipType shipType) {
        switch (shipType) {
            case FOUR_DECK:
                findViewById(R.id.ship_four_deck).setEnabled(false);
                break;
            case THREE_DECK:
                findViewById(R.id.ship_three_deck).setEnabled(false);
                break;
            case TWO_DECK:
                findViewById(R.id.ship_two_deck).setEnabled(false);
                break;
            case ONE_DECK:
                findViewById(R.id.ship_one_deck).setEnabled(false);
                break;
            case MINE:
                findViewById(R.id.ship_mine).setEnabled(false);
                break;
        }
    }

    private void enableShipButton(ShipType shipType) {
        switch (shipType) {
            case FOUR_DECK:
                findViewById(R.id.ship_four_deck).setEnabled(true);
                break;
            case THREE_DECK:
                findViewById(R.id.ship_three_deck).setEnabled(true);
                break;
            case TWO_DECK:
                findViewById(R.id.ship_two_deck).setEnabled(true);
                break;
            case ONE_DECK:
                findViewById(R.id.ship_one_deck).setEnabled(true);
                break;
            case MINE:
                findViewById(R.id.ship_mine).setEnabled(true);
                break;
        }
    }

    private void enableAllShipButtons() {
        for (ShipType shipType : ShipType.values()) {
            enableShipButton(shipType);
        }
    }

    private boolean areAllShipsPlaced() {
        for (int quantity : shipQuantities.values()) {
            if (quantity > 0) {
                return false;
            }
        }
        return true;
    }

    private void updatePlayerGrid() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                Cell cell = playerBoard.cells[i][j];
                Button cellButton = (Button) playerGrid.getChildAt(i * GRID_SIZE + j);
                if (cell.ship != null) {
                    if (cell.ship.isMine) {
                        cellButton.setBackgroundColor(Color.RED);
                    } else {
                        cellButton.setBackgroundColor(Color.GREEN);
                    }
                } else {
                    cellButton.setBackgroundColor(Color.WHITE);
                }
            }
        }
    }

    private void startGame() {
        gameStarted = true;
        shipSelectionGrid.setVisibility(View.GONE);
        rotateButton.setVisibility(View.GONE);
        shipSelectionLabel.setVisibility(View.GONE);
        startGameButton.setVisibility(View.GONE);

        opponentGrid.setVisibility(View.VISIBLE);
        findViewById(R.id.opponentLabel).setVisibility(View.VISIBLE);

        opponentBoard.placeShips();
        placeMinesForOpponent();

        createGridCells(opponentGrid, true);
    }

    private void placeMinesForOpponent() {
        while (opponentMines.size() < 2) {
            int x = random.nextInt(GRID_SIZE);
            int y = random.nextInt(GRID_SIZE);
            Point point = new Point(x, y);

            if (!isAdjacentToShip(opponentBoard, x, y) && !isAdjacentToMine(opponentMines, x, y) && !opponentMines.contains(point)) {
                opponentMines.add(point);
            }
        }
    }

    private boolean isAdjacentToMine(List<Point> mines, int x, int y) {
        for (Point mine : mines) {
            int dx = Math.abs(mine.x - x);
            int dy = Math.abs(mine.y - y);
            if (dx <= 1 && dy <= 1) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdjacentToShip(Board board, int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE) {
                    if (board.cells[nx][ny].hasShip) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void handlePlayerMove(Button cellButton, int x, int y) {
        if (currentTurn != Turn.PLAYER) {
            return;
        }

        Cell cell = opponentBoard.cells[x][y];

        if (cell.isHit) {
            return;
        }

        cell.isHit = true;
        cellButton.setEnabled(false);

        if (cell.hasShip) {
            cellButton.setBackgroundColor(Color.parseColor("#FFA500")); // Оранжевый: попадание
            Ship hitShip = findShipByCell(opponentBoard, cell);
            if (hitShip.isSunk()) {
                colorSurroundingCells(hitShip, opponentBoard, opponentGrid, Color.GRAY); // Закрашиваем клетки вокруг потопленного корабля
                for (Cell shipCell : hitShip.cells) {
                    Button shipButton = (Button) opponentGrid.getChildAt(shipCell.x * GRID_SIZE + shipCell.y);
                    shipButton.setBackgroundColor(Color.YELLOW); // Жёлтый: потопленный корабль
                }
            }

            if (checkAllShipsSunk(opponentBoard)) {
                endGame(true); // Игра окончена, вы выиграли
                return;
            }
        } else {
            cellButton.setBackgroundColor(Color.GRAY); // Серый: промах
        }

        // Проверяем, попал ли на мину
        if (opponentMines.contains(new Point(x, y))) {
            playerMustSkip = true;
            cellButton.setBackgroundColor(Color.parseColor("#8B0000")); // Тёмно-красный: попадание в мину
            Toast.makeText(this, "Мина! Вы пропускаете следующий ход.", Toast.LENGTH_SHORT).show();

            // Закрашиваем клетки вокруг мины
            colorSurroundingCells(x, y, opponentBoard, opponentGrid, Color.GRAY);
        }

        // Определяем, кто ходит следующим
        if (cell.hasShip) {
            currentTurn = Turn.PLAYER; // Игрок продолжает ходить при попадании
        } else {
            currentTurn = Turn.COMPUTER; // Ход переходит к компьютеру при промахе
            handler.postDelayed(this::computerMove, 500);
        }
    }

    private void computerMove() {
        if (currentTurn != Turn.COMPUTER) {
            return;
        }

        if (computerMustSkip) {
            computerMustSkip = false;
            currentTurn = Turn.PLAYER;
            return;
        }

        Point target = getNextComputerTarget();
        int x = target.x;
        int y = target.y;
        Cell cell = playerBoard.cells[x][y];
        cell.isHit = true;

        int index = x * GRID_SIZE + y;
        Button cellButton = (Button) playerGrid.getChildAt(index);
        cellButton.setEnabled(false);

        if (cell.hasShip) {
            cellButton.setBackgroundColor(Color.parseColor("#FFA500")); // Оранжевый: попадание
            Ship hitShip = findShipByCell(playerBoard, cell);

            if (hitShip.isSunk()) {
                colorSurroundingCells(hitShip, playerBoard, playerGrid, Color.GRAY); // Закрашиваем серым
                for (Cell shipCell : hitShip.cells) {
                    Button shipButton = (Button) playerGrid.getChildAt(shipCell.x * GRID_SIZE + shipCell.y);
                    shipButton.setBackgroundColor(Color.YELLOW); // Жёлтый для потопленного корабля
                }

                lastHitPoint = null;
                potentialTargets.clear();
                currentOrientation = Orientation.UNKNOWN; // Сбрасываем ориентацию

                if (checkAllShipsSunk(playerBoard)) {
                    endGame(false); // Компьютер победил
                    return;
                }
            } else {
                // Корабль ещё не потоплен, добавляем новые цели
                addSurroundingTargets(x, y);
                lastHitPoint = new Point(x, y); // Обновляем lastHitPoint здесь
            }

            // Компьютер продолжает ходить
            handler.postDelayed(this::computerMove, 500);
            return;
        } else {
            cellButton.setBackgroundColor(Color.GRAY); // Серый: промах
        }

        // Проверяем, попал ли на мину
        if (playerMines.contains(new Point(x, y))) {
            computerMustSkip = true;
            cellButton.setBackgroundColor(Color.parseColor("#8B0000")); // Тёмно-красный: попадание в мину
            Toast.makeText(this, "Мина! Компьютер пропускает следующий ход.", Toast.LENGTH_SHORT).show();

            // Закрашиваем клетки вокруг мины
            colorSurroundingCells(x, y, playerBoard, playerGrid, Color.GRAY);
        }

        // Передаём ход игроку
        currentTurn = Turn.PLAYER;
    }

    private void colorSurroundingCells(int x, int y, Board board, GridLayout grid, int color) {
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];

            if (nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE) {
                Cell neighbor = board.cells[nx][ny];
                if (!neighbor.isHit) {
                    neighbor.isHit = true;
                    int index = nx * GRID_SIZE + ny;
                    Button neighborButton = (Button) grid.getChildAt(index);
                    neighborButton.setEnabled(false);
                    neighborButton.setBackgroundColor(color);
                }
            }
        }
    }

    private void colorSurroundingCells(Ship ship, Board board, GridLayout gridLayout, int color) {
        for (Cell cell : ship.cells) {
            int x = cell.x;
            int y = cell.y;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = x + dx;
                    int ny = y + dy;

                    if (nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE) {
                        Cell surroundingCell = board.cells[nx][ny];
                        if (!surroundingCell.hasShip && !surroundingCell.isHit) {
                            surroundingCell.isHit = true;
                            Button surroundingButton = (Button) gridLayout.getChildAt(nx * GRID_SIZE + ny);
                            surroundingButton.setEnabled(false);
                            surroundingButton.setBackgroundColor(color);
                        }
                    }
                }
            }
        }
    }

    private Point getNextComputerTarget() {
        // Если есть приоритетные цели, выбираем из них
        if (!potentialTargets.isEmpty()) {
            return potentialTargets.remove(0);
        }

        // Если целей нет, выбираем случайную клетку
        int x, y;
        Cell cell;
        do {
            x = random.nextInt(GRID_SIZE);
            y = random.nextInt(GRID_SIZE);
            cell = playerBoard.cells[x][y];
        } while (cell.isHit);

        return new Point(x, y);
    }

    private void addSurroundingTargets(int x, int y) {
        if (lastHitPoint != null) {
            // Если есть два попадания, определяем ориентацию
            if (currentOrientation == Orientation.UNKNOWN) {
                if (lastHitPoint.x == x) {
                    currentOrientation = Orientation.HORIZONTAL;
                } else if (lastHitPoint.y == y) {
                    currentOrientation = Orientation.VERTICAL;
                }

                if (currentOrientation != Orientation.UNKNOWN) {
                    // После определения ориентации, удаляем цели в неправильном направлении
                    filterPotentialTargets();
                }
            }

            // Ограничиваем направления для новых целей
            if (currentOrientation == Orientation.HORIZONTAL) {
                addTargetIfValid(x, y - 1); // Влево
                addTargetIfValid(x, y + 1); // Вправо
            } else if (currentOrientation == Orientation.VERTICAL) {
                addTargetIfValid(x - 1, y); // Вверх
                addTargetIfValid(x + 1, y); // Вниз
            }
        } else {
            // Первый выстрел — добавляем все четыре направления
            addTargetIfValid(x - 1, y); // Вверх
            addTargetIfValid(x + 1, y); // Вниз
            addTargetIfValid(x, y - 1); // Влево
            addTargetIfValid(x, y + 1); // Вправо
        }
    }

    private void filterPotentialTargets() {
        List<Point> filteredTargets = new ArrayList<>();

        for (Point target : potentialTargets) {
            if (currentOrientation == Orientation.HORIZONTAL && target.x == lastHitPoint.x) {
                filteredTargets.add(target);
            } else if (currentOrientation == Orientation.VERTICAL && target.y == lastHitPoint.y) {
                filteredTargets.add(target);
            }
        }

        potentialTargets = filteredTargets;
    }

    // Добавление цели, если она подходит
    private void addTargetIfValid(int x, int y) {
        if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE) {
            Cell neighbor = playerBoard.cells[x][y];
            if (!neighbor.isHit) {
                Point target = new Point(x, y);
                if (!potentialTargets.contains(target)) {
                    potentialTargets.add(target);
                }
            }
        }
    }

    private Ship findShipByCell(Board board, Cell cell) {
        for (Ship ship : board.ships) {
            if (ship.cells.contains(cell)) {
                return ship;
            }
        }
        return null;
    }

    private boolean checkAllShipsSunk(Board board) {
        for (Ship ship : board.ships) {
            if (!ship.isSunk()) {
                return false;
            }
        }
        return true;
    }

    private void endGame(boolean playerWon) {
        currentTurn = null; // Игра окончена

        // Блокируем все клетки
        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            Button opponentButton = (Button) opponentGrid.getChildAt(i);
            opponentButton.setEnabled(false);
            Button playerButton = (Button) playerGrid.getChildAt(i);
            playerButton.setEnabled(false);
        }

        // Показываем диалог с результатом и кнопкой перезапуска
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(playerWon ? "Победа!" : "Поражение");
        builder.setMessage(playerWon ? "Вы потопили все корабли противника!" : "Ваши корабли были потоплены.");

        builder.setPositiveButton("Играть снова", (dialogInterface, i) -> restartGame());
        builder.setCancelable(false);
        builder.show();
    }

    private void restartGame() {
        startNewGame();
    }
}
