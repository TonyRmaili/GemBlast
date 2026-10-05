package gemblast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Everything that happened in ONE spin: the first grid, then each avalanche step,
 * then (super free spins only) the row BONUS on the final grid, then (free spins) the SHATTER explosions.
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
        public final int multiplierAfter;    // the multiplier once this step is done
        public final Map<Symbol, Integer> shattered;   // free spins: gems collected in this step (SlotMath.shatterCollect)

        public Cascade(List<Win> wins, boolean[][] removed, Symbol[][] gridAfter, boolean[][] newCells,
                       boolean[][] stickyAfter, int multiplier, int multiplierAfter, Map<Symbol, Integer> shattered) {
            this.wins = Collections.unmodifiableList(wins);
            this.removed = removed;
            this.gridAfter = gridAfter;
            this.newCells = newCells;
            this.stickyAfter = stickyAfter;
            this.multiplier = multiplier;
            this.multiplierAfter = multiplierAfter;
            Map<Symbol, Integer> copy = new EnumMap<>(Symbol.class);   // own copy, in enum order
            copy.putAll(shattered);
            this.shattered = Collections.unmodifiableMap(copy);
        }

        /** How many of this gem were collected in this step (0 if none). */
        public int shatteredCount(Symbol gem) {
            return shattered.getOrDefault(gem, 0);
        }

        /** One win's amount in cents, including this step's multiplier. */
        public long amount(Win win, long betCents) {
            return win.amount(betCents) * multiplier;
        }

        /** This step's total as a multiple of the bet, including the multiplier. */
        public double totalMultiplier() {
            double total = 0.0;
            for (Win win : wins) {
                total += win.multiplier();
            }
            return total * multiplier;
        }
    }

    public final Symbol[][] initialGrid;
    public final boolean[][] stickyBefore;   // sticky wilds carried in from earlier spins
    public final boolean[][] stickyLanded;   // sticky wilds once the first grid has landed
    public final List<Cascade> cascades;     // empty = this spin didn't win anything
    public final int multiplierAfter;        // multiplier carried into the next spin
    public final int scatterCount;           // scatters on the final grid (counted by SlotMath)
    /**
     * Row BONUS per row of the final grid, x bet BEFORE the multiplier (from SlotMath.uniqueGemsInRowPay).
     * 0 = that row didn't pay. Always all 0 outside super free spins.
     * The multiplier that applies is multiplierAfter: the one reached at the end of the spin.
     */
    private final double[] rowBonus;
    /**
     * SHATTER (both free spins modes). The meters per gem, indexed by Symbol.ordinal():
     * before this spin, and after this spin's explosions. Both null in the base game (feature off).
     */
    public final int[] shatterMetersBefore;
    public final int[] shatterMetersAfter;
    private final List<Symbol> shatterGems;    // gems that exploded at the end of this spin, in order
    private final List<Double> shatterPays;    // their pays, x bet, multiplier included (SlotMath.shatterPay)

    public SpinResult(Symbol[][] initialGrid, boolean[][] stickyBefore, boolean[][] stickyLanded,
                      List<Cascade> cascades, int multiplierAfter, int scatterCount, double[] rowBonus,
                      int[] shatterMetersBefore, int[] shatterMetersAfter,
                      List<Symbol> shatterGems, List<Double> shatterPays) {
        this.initialGrid = initialGrid;
        this.stickyBefore = stickyBefore;
        this.stickyLanded = stickyLanded;
        this.cascades = Collections.unmodifiableList(cascades);
        this.multiplierAfter = multiplierAfter;
        this.scatterCount = scatterCount;
        this.rowBonus = rowBonus.clone();
        this.shatterMetersBefore = shatterMetersBefore == null ? null : shatterMetersBefore.clone();
        this.shatterMetersAfter = shatterMetersAfter == null ? null : shatterMetersAfter.clone();
        this.shatterGems = Collections.unmodifiableList(new ArrayList<>(shatterGems));
        this.shatterPays = Collections.unmodifiableList(new ArrayList<>(shatterPays));
    }

    // ---------------------------------------------------------------- SHATTER

    /** True in free spins (the meters exist), false in the base game. */
    public boolean hasShatterMeters() {
        return shatterMetersBefore != null;
    }

    /** How many explosions happened at the end of this spin. */
    public int shatterCount() {
        return shatterGems.size();
    }

    public Symbol shatterGem(int index) {
        return shatterGems.get(index);
    }

    /** One explosion's pay, x bet, multiplier included. */
    public double shatterPay(int index) {
        return shatterPays.get(index);
    }

    /** One explosion's pay in cents. */
    public long shatterAmount(int index, long betCents) {
        return Math.round(shatterPays.get(index) * betCents);
    }

    /** All explosions of this spin, x bet. */
    public double shatterMultiplier() {
        double total = 0.0;
        for (double pay : shatterPays) {
            total += pay;
        }
        return total;
    }

    // ---------------------------------------------------------------- row BONUS

    public int rowCount() {
        return rowBonus.length;
    }

    /** True if this row of the final grid paid a row BONUS. */
    public boolean hasRowBonus(int row) {
        return rowBonus[row] > 0;
    }

    public boolean hasAnyRowBonus() {
        for (double pay : rowBonus) {
            if (pay > 0) {
                return true;
            }
        }
        return false;
    }

    /** Row BONUS pay of one row, x bet, before the multiplier. */
    public double rowBonusBasePay(int row) {
        return rowBonus[row];
    }

    /** Row BONUS of one row in cents, including the multiplier (same rounding as a normal win). */
    public long rowBonusAmount(int row, long betCents) {
        return Math.round(rowBonus[row] * betCents) * multiplierAfter;
    }

    /** All row BONUS pays of this spin, x bet, including the multiplier. */
    public double rowBonusMultiplier() {
        double total = 0.0;
        for (double pay : rowBonus) {
            total += pay;
        }
        return total * multiplierAfter;
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

    public double totalMultiplier() {
        double total = 0.0;
        for (Cascade cascade : cascades) {
            total += cascade.totalMultiplier();
        }
        return total + rowBonusMultiplier() + shatterMultiplier();
    }

    /** Total in cents: the sum of each shown amount, so it matches the screen exactly. */
    public long totalWin(long betCents) {
        long total = 0;
        for (Cascade cascade : cascades) {
            for (Win win : cascade.wins) {
                total += cascade.amount(win, betCents);
            }
        }
        for (int row = 0; row < rowBonus.length; row++) {
            total += rowBonusAmount(row, betCents);
        }
        for (int i = 0; i < shatterPays.size(); i++) {
            total += shatterAmount(i, betCents);
        }
        return total;
    }

    public boolean isWin() {
        return !cascades.isEmpty() || hasAnyRowBonus() || !shatterGems.isEmpty();
    }
}