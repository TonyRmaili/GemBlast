package gemblast;

/**
 * Every feature rule in one place. Change a number here and the engine, screen, info page
 * and simulator all follow.
 */
public final class GameRules {

    private GameRules() { }

    // --- triggers (scatters are counted on the FINAL grid of a spin, after all avalanches) ---
    public static final int FREE_SPINS_SCATTERS = 3;   // 3 scatters -> free spins
    public static final int SUPER_SCATTERS      = 4;   // 4 or more  -> super free spins

    // --- free spins ---
    public static final int FREE_SPINS_AWARDED  = 10;
    public static final int RETRIGGER_SCATTERS  = 3;   // during free spins: 3+ scatters ...
    public static final int RETRIGGER_SPINS     = 5;   // ... add 5 spins

    // --- persistent multiplier (free spins only; the base game is always x1) ---
    public static final int MULTIPLIER_START = 1;
    public static final int MULTIPLIER_STEP  = 1;      // added after every winning avalanche step

    // --- super free spins: sticky wilds (a wild locks in place until it is part of a win) ---
    public static final int MAX_STICKY_WILDS = 12;     // tuning knob; 12 = every cell of reels 2-5 (no real limit)

    // --- safety limits: practically never reached, but loops must always end ---
    public static final int MAX_CASCADES   = 100;
    public static final int MAX_FREE_SPINS = 200;
}