package gemblast;

import java.util.Collections;
import java.util.List;

/**
 * Everything that happened in ONE spin: the first grid, then each avalanche step.
 * (This was called RoundResult before free spins existed; a round can now contain many spins.)
 * Pure data. The screen replays it; the simulator only reads the numbers.
 */
public final class SpinResult {

    /**
     * One avalanche step: the wins found, which cells were removed, the grid after the refill,
     * and the multiplier that applied to these wins.
     */
    public static final class Cascade {
        public final List<Win> wins;
        public final boolean[][] removed;    // [reel][row] cells cleared in this step
        public final Symbol[][] gridAfter;   // grid after the fall + refill
        public final boolean[][] newCells;   // [reel][row] cells that received a new symbol
        public final boolean[][] stickyAfter;// sticky wilds after this step (super free spins)
        public final int multiplier;         // applied to every win in this step (always 1 in the base game)
        public final int multiplierAfter;    // the multiplier once this step is done (grows in free spins)

        public Cascade(List<Win> wins, boolean[][] removed, Symbol[][] gridAfter, boolean[][] newCells,
                       boolean[][] stickyAfter, int multiplier, int multiplierAfter) {
            this.wins = Collections.unmodifiableList(wins);
            this.removed = removed;
            this.gridAfter = gridAfter;
            this.newCells = newCells;
            this.stickyAfter = stickyAfter;
            this.multiplier = multiplier;
            this.multiplierAfter = multiplierAfter;
        }

        /** One win's amount in cents, including this step's multiplier. */
        public long amount(Win win, long betCents) {
            return win.amount(betCents) * multiplier;
        }

        /** This step's total as a multiple of the bet, including the multiplier. */
        public double totalMultiplier() {
            return WinEvaluator.totalMultiplier(wins) * multiplier;
        }
    }

    public final Symbol[][] initialGrid;
    public final boolean[][] stickyBefore;   // sticky wilds carried in from earlier spins
    public final boolean[][] stickyLanded;   // sticky wilds once the first grid has landed (new wilds locked)
    public final List<Cascade> cascades;     // empty = this spin didn't win anything
    public final int multiplierAfter;        // multiplier carried into the next spin

    public SpinResult(Symbol[][] initialGrid, boolean[][] stickyBefore, boolean[][] stickyLanded,
                      List<Cascade> cascades, int multiplierAfter) {
        this.initialGrid = initialGrid;
        this.stickyBefore = stickyBefore;
        this.stickyLanded = stickyLanded;
        this.cascades = Collections.unmodifiableList(cascades);
        this.multiplierAfter = multiplierAfter;
    }

    /** Sticky wilds when the spin is over. */
    public boolean[][] finalSticky() {
        return cascades.isEmpty() ? stickyLanded : cascades.get(cascades.size() - 1).stickyAfter;
    }

    public int finalStickyCount() {
        int count = 0;
        for (boolean[] reel : finalSticky()) {
            for (boolean locked : reel) {
                if (locked) {
                    count++;
                }
            }
        }
        return count;
    }

    /** The grid left on screen when the spin is over (after the last avalanche). */
    public Symbol[][] finalGrid() {
        return cascades.isEmpty() ? initialGrid : cascades.get(cascades.size() - 1).gridAfter;
    }

    /** Scatters on the final grid: that's what decides free spins. */
    public int scatterCount() {
        int count = 0;
        for (Symbol[] reel : finalGrid()) {
            for (Symbol symbol : reel) {
                if (symbol == Symbol.SCATTER) {
                    count++;
                }
            }
        }
        return count;
    }

    public double totalMultiplier() {
        double total = 0.0;
        for (Cascade cascade : cascades) {
            total += cascade.totalMultiplier();
        }
        return total;
    }

    /** Total in cents: the sum of each shown amount, so it matches the screen exactly. */
    public long totalWin(long betCents) {
        long total = 0;
        for (Cascade cascade : cascades) {
            for (Win win : cascade.wins) {
                total += cascade.amount(win, betCents);
            }
        }
        return total;
    }

    public boolean isWin() {
        return !cascades.isEmpty();
    }
}