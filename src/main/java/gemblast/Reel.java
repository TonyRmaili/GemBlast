package gemblast;

import com.badlogic.gdx.scenes.scene2d.Group;

/** One vertical column of cells. A Group so it can hold child actors (the cells). */
public class Reel extends Group {

    private final Cell[] cells;

    public Reel(int rowCount, float cellSize, float gap) {
        setTransform(false);
        setSize(cellSize, rowCount * cellSize + (rowCount - 1) * gap);

        cells = new Cell[rowCount];
        for (int row = 0; row < rowCount; row++) {
            Cell cell = new Cell(cellSize);
            // libGDX y goes UP, but row 0 is the TOP row, so we place from the top down.
            float y = getHeight() - (row + 1) * cellSize - row * gap;
            cell.setPosition(0f, y);
            cells[row] = cell;
            addActor(cell);
        }
    }

    /** symbols[0] is the top row. */
    public void showSymbols(Symbol[] symbols) {
        for (int row = 0; row < cells.length; row++) {
            cells[row].setSymbol(symbols[row]);
        }
    }

    public Cell getCell(int row) {
        return cells[row];
    }

    public void setEmpty() {
        for (Cell cell : cells) {
            cell.setEmpty();
        }
    }
}