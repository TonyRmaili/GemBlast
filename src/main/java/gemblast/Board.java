package gemblast;

import java.util.Arrays;

/**
 * The current grid: which symbol sits in every cell, and which cells hold a sticky wild.
 * Only storage, no rules: SlotMath decides what goes in it.
 *
 * Indexing: [reel][row], reel 0 = leftmost, row 0 = TOP row.
 */
public final class Board {

    public final int reelCount;
    public final int rowCount;
    private final Symbol[][] cells;
    private final boolean[][] sticky;

    public Board(int reelCount, int rowCount) {
        this.reelCount = reelCount;
        this.rowCount = rowCount;
        this.cells = new Symbol[reelCount][rowCount];
        this.sticky = new boolean[reelCount][rowCount];
    }

    public Symbol get(int reel, int row) {
        return cells[reel][row];
    }

    public void set(int reel, int row, Symbol symbol) {
        cells[reel][row] = symbol;
    }

    public boolean isSticky(int reel, int row) {
        return sticky[reel][row];
    }

    public void setSticky(int reel, int row, boolean locked) {
        sticky[reel][row] = locked;
    }

    /** Unlocks every cell (start of every round and every bonus). */
    public void clearSticky() {
        for (boolean[] reel : sticky) {
            Arrays.fill(reel, false);
        }
    }

    /** A full copy of the grid, so later changes to the board don't affect it. */
    public Symbol[][] snapshot() {
        Symbol[][] copy = new Symbol[reelCount][];
        for (int reel = 0; reel < reelCount; reel++) {
            copy[reel] = cells[reel].clone();
        }
        return copy;
    }

    public boolean[][] stickySnapshot() {
        boolean[][] copy = new boolean[reelCount][];
        for (int reel = 0; reel < reelCount; reel++) {
            copy[reel] = sticky[reel].clone();
        }
        return copy;
    }

    /** Prints the grid as text, handy for checking results in the console. (* = sticky) */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < rowCount; row++) {
            for (int reel = 0; reel < reelCount; reel++) {
                String name = cells[reel][row] + (sticky[reel][row] ? "*" : "");
                sb.append(String.format("%-14s", name));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}