package gemblast;

import java.util.Collections;
import java.util.List;

/** Everything that happened in one free spins bonus. Pure data. */
public final class FreeSpinsResult {

    public final boolean superMode;          // triggered with 4+ scatters: sticky wilds on top
    public final List<SpinResult> spins;     // in order, including retriggered spins

    public FreeSpinsResult(boolean superMode, List<SpinResult> spins) {
        this.superMode = superMode;
        this.spins = Collections.unmodifiableList(spins);
    }

    public int spinCount() {
        return spins.size();
    }

    /** True if this spin (by index) awarded extra spins. */
    public boolean retriggeredAt(int index) {
        return spins.get(index).scatterCount() >= GameRules.RETRIGGER_SCATTERS;
    }

    public int retriggerCount() {
        int count = 0;
        for (int i = 0; i < spins.size(); i++) {
            if (retriggeredAt(i)) {
                count++;
            }
        }
        return count;
    }

    /** The multiplier reached by the end of the bonus. */
    public int finalMultiplier() {
        return spins.isEmpty() ? GameRules.MULTIPLIER_START : spins.get(spins.size() - 1).multiplierAfter;
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