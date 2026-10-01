package gemblast;

import java.util.Arrays;
import java.util.Random;

/**
 * The current grid: which symbol sits in every cell, plus the sticky wilds of super free spins.
 * Pure data, no graphics (the simulator uses this too).
 *
 * Indexing: cells[reel][row], reel 0 = leftmost, row 0 = TOP row.
 */
public final class Board {

    public final int reelCount;
    public final int rowCount;
    private final Symbol[][] cells;
    private final boolean[][] sticky;   // super free spins: a locked wild that stays until it is part of a win

    public Board(int reelCount, int rowCount) {
        this.reelCount = reelCount;
        this.rowCount = rowCount;
        this.cells = new Symbol[reelCount][rowCount];
        this.sticky = new boolean[reelCount][rowCount];
    }

    /** Fills every cell with a fresh random symbol, except sticky cells, which keep their wild. */
    public void spin(ReelWeights weights, Random rng) {
        for (int reel = 0; reel < reelCount; reel++) {
            for (int row = 0; row < rowCount; row++) {
                if (!sticky[reel][row]) {
                    cells[reel][row] = weights.draw(reel, rng);
                }
            }
        }
    }

    /** Puts a specific symbol in one cell (used by the boosters). */
    public void set(int reel, int row, Symbol symbol) {
        cells[reel][row] = symbol;
    }

    public Symbol get(int reel, int row) {
        return cells[reel][row];
    }

    /** A copy of one reel's symbols (top to bottom). A copy so nobody can change the board by accident. */
    public Symbol[] getReel(int reel) {
        return cells[reel].clone();
    }

    /** A full copy of the grid [reel][row], so later changes to the board don't affect it. */
    public Symbol[][] snapshot() {
        Symbol[][] copy = new Symbol[reelCount][];
        for (int reel = 0; reel < reelCount; reel++) {
            copy[reel] = cells[reel].clone();
        }
        return copy;
    }

    // ---------------------------------------------------------------- sticky wilds (super free spins)

    /**
     * Locks every wild currently on the grid, as long as the total stays within maxSticky.
     * Reels are checked left to right, so if the limit is hit, the leftmost wilds are locked first.
     */
    public void lockWilds(int maxSticky) {
        int count = stickyCount();
        for (int reel = 0; reel < reelCount; reel++) {
            for (int row = 0; row < rowCount; row++) {
                if (count >= maxSticky) {
                    return;
                }
                if (cells[reel][row] == Symbol.WILD && !sticky[reel][row]) {
                    sticky[reel][row] = true;
                    count++;
                }
            }
        }
    }

    /** Unlocks everything (start of every round and every bonus). */
    public void clearSticky() {
        for (boolean[] reel : sticky) {
            Arrays.fill(reel, false);
        }
    }

    public boolean[][] stickySnapshot() {
        boolean[][] copy = new boolean[reelCount][];
        for (int reel = 0; reel < reelCount; reel++) {
            copy[reel] = sticky[reel].clone();
        }
        return copy;
    }

    public int stickyCount() {
        int count = 0;
        for (boolean[] reel : sticky) {
            for (boolean locked : reel) {
                if (locked) {
                    count++;
                }
            }
        }
        return count;
    }

    // ---------------------------------------------------------------- avalanche

    /**
     * The avalanche: removes the marked cells, lets the symbols above fall down,
     * and fills the gaps at the top with new random symbols.
     *
     * Sticky wilds:
     *   - part of a win (marked for removal): used up, unlocked and removed like any symbol
     *   - not part of a win: stay exactly where they are; other symbols fall PAST them
     *
     * Example reel (top to bottom): [Ruby, Opal*, Topaz]  (* = removed)
     *   kept, in order:  [Ruby, Topaz]
     *   after the fall:  [new, Ruby, Topaz]
     * With a sticky wild W that did not win: [Ruby*, W, Topaz]
     *   free slots = rows 0 and 2, kept = [Topaz] -> [new, W, Topaz]
     *
     * @return newCells[reel][row] = true where a new symbol came in (the screen uses it to animate)
     */
    public boolean[][] avalanche(boolean[][] remove, ReelWeights weights, Random rng) {
        boolean[][] newCells = new boolean[reelCount][rowCount];

        for (int reel = 0; reel < reelCount; reel++) {
            // 1) free slots (everything not locked) and the survivors among them, both top to bottom
            int[] freeRows = new int[rowCount];
            int freeCount = 0;
            Symbol[] kept = new Symbol[rowCount];
            int keptCount = 0;
            for (int row = 0; row < rowCount; row++) {
                if (sticky[reel][row] && remove[reel][row]) {
                    sticky[reel][row] = false;                // the sticky wild won: it's used up
                }
                if (sticky[reel][row]) {
                    continue;                                 // still locked: stays exactly where it is
                }
                freeRows[freeCount++] = row;                  // store, then add 1 to freeCount
                if (!remove[reel][row]) {
                    kept[keptCount++] = cells[reel][row];
                }
            }

            // 2) new symbols go in the top free slots, survivors fill the bottom free slots in order
            int newCount = freeCount - keptCount;
            for (int i = 0; i < freeCount; i++) {
                int row = freeRows[i];
                if (i < newCount) {
                    cells[reel][row] = weights.draw(reel, rng);
                    newCells[reel][row] = true;
                } else {
                    cells[reel][row] = kept[i - newCount];
                }
            }
        }
        return newCells;
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