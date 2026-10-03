package gemblast;

/**
 * One PAID round: the base spin, plus the free spins bonus if it triggered.
 * This split is exactly the "main game" vs "bonus game" split the statistics need.
 * A bought bonus has no base spin (baseSpin == null): it goes straight into free spins.
 */
public final class RoundResult {

    public final GameMode mode;               // how this round was played
    public final SpinResult baseSpin;         // null for a bought bonus
    public final FreeSpinsResult freeSpins;   // null when the bonus didn't trigger

    public RoundResult(GameMode mode, SpinResult baseSpin, FreeSpinsResult freeSpins) {
        this.mode = mode;
        this.baseSpin = baseSpin;
        this.freeSpins = freeSpins;
    }

    public boolean hasBaseSpin() {
        return baseSpin != null;
    }

    public boolean triggeredFreeSpins() {
        return freeSpins != null;
    }

    /** Main game part (base spin + its avalanches), as a multiple of the bet. */
    public double baseMultiplier() {
        return baseSpin == null ? 0.0 : baseSpin.totalMultiplier();
    }

    /** Bonus part, as a multiple of the bet (0 if no bonus). */
    public double bonusMultiplier() {
        return freeSpins == null ? 0.0 : freeSpins.totalMultiplier();
    }

    public double totalMultiplier() {
        return baseMultiplier() + bonusMultiplier();
    }

    public long totalWin(long betCents) {
        long total = baseSpin == null ? 0 : baseSpin.totalWin(betCents);
        if (freeSpins != null) {
            total += freeSpins.totalWin(betCents);
        }
        return total;
    }
}
