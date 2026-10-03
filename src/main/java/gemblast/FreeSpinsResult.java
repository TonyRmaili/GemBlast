package gemblast;

import java.util.Collections;
import java.util.List;

/** Everything that happened in one free spins bonus. Pure data. */
public final class FreeSpinsResult {

    public final boolean superMode;          // super free spins: sticky wilds on top
    public final int initialSpins;           // spins awarded when the bonus started
    public final int startMultiplier;
    public final List<SpinResult> spins;     // in order, including retriggered spins
    private final List<Integer> extraSpins;  // extraSpins.get(i) = spins added by spin i (0 = none)

    public FreeSpinsResult(boolean superMode, int initialSpins, int startMultiplier,
                           List<SpinResult> spins, List<Integer> extraSpins) {
        this.superMode = superMode;
        this.initialSpins = initialSpins;
        this.startMultiplier = startMultiplier;
        this.spins = Collections.unmodifiableList(spins);
        this.extraSpins = Collections.unmodifiableList(extraSpins);
    }

    public int spinCount() {
        return spins.size();
    }

    /** Spins added by the spin at this index (0 = no retrigger). */
    public int extraSpinsAt(int index) {
        return extraSpins.get(index);
    }

    public int retriggerCount() {
        int count = 0;
        for (int extra : extraSpins) {
            if (extra > 0) {
                count++;
            }
        }
        return count;
    }

    /** The multiplier reached by the end of the bonus. */
    public int finalMultiplier() {
        return spins.isEmpty() ? startMultiplier : spins.get(spins.size() - 1).multiplierAfter;
    }

    /** Sticky wilds left on the grid at the end of the bonus (always 0 in regular free spins). */
    public int finalStickyWilds() {
        return spins.isEmpty() ? 0 : spins.get(spins.size() - 1).finalStickyCount();
    }

    public double totalMultiplier() {
        double total = 0.0;
        for (SpinResult spin : spins) {
            total += spin.totalMultiplier();
        }
        return total;
    }

    public long totalWin(long betCents) {
        long total = 0;
        for (SpinResult spin : spins) {
            total += spin.totalWin(betCents);
        }
        return total;
    }
}