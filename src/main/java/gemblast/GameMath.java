package gemblast;

/**
 * Exact (calculated, not simulated) numbers for the current weights and paytable.
 * Later, the simulator's results must match these. That is how we prove the game code is correct.
 *
 * All values are for ONE drop: no avalanche, no free spins yet.
 */
public final class GameMath {

    private GameMath() { }

    /**
     * Expected return from one symbol, as a fraction of the bet (0.05 = 5% RTP).
     *
     * For "exactly k of a kind" (reels 1..k have the symbol, reel k+1 doesn't):
     *   E = pay(k) * (rows*q1) * (rows*q2) * ... * (rows*qk) * (1 - q_{k+1})^rows
     * q = chance one cell shows the symbol OR a wild.
     * rows*q = expected number of matching cells on that reel (each one multiplies the ways).
     * (1-q)^rows = chance the next reel has none, so the win stops at k.
     */
    public static double symbolRtp(ReelWeights weights, Paytable paytable, Symbol symbol, int rows) {
        int reels = weights.reelCount();
        double total = 0.0;

        for (int k = Paytable.MIN_COUNT; k <= reels; k++) {
            double expected = paytable.pay(symbol, k);
            if (expected == 0.0) {
                continue;
            }
            for (int r = 0; r < k; r++) {
                expected *= rows * matchChance(weights, r, symbol);
            }
            if (k < reels) {
                expected *= Math.pow(1.0 - matchChance(weights, k, symbol), rows);
            }
            total += expected;
        }
        return total;
    }

    /** Sum of symbolRtp over every paying symbol. */
    public static double waysRtp(ReelWeights weights, Paytable paytable, int rows) {
        double total = 0.0;
        for (Symbol symbol : Symbol.values()) {
            if (paytable.isPaying(symbol)) {
                total += symbolRtp(weights, paytable, symbol, rows);
            }
        }
        return total;
    }

    /**
     * Chance of at least `minCount` scatters anywhere on the grid.
     *
     * Works cell by cell: dist[n] = chance of having seen exactly n scatters so far.
     * Each new cell either is a scatter (n -> n+1) or isn't (n stays).
     * This handles reels with different scatter weights, which a simple binomial can't.
     */
    public static double scatterChance(ReelWeights weights, int rows, int minCount) {
        int cells = weights.reelCount() * rows;
        double[] dist = new double[cells + 1];
        dist[0] = 1.0;

        for (int reel = 0; reel < weights.reelCount(); reel++) {
            double p = weights.probability(reel, Symbol.SCATTER);
            for (int row = 0; row < rows; row++) {
                double[] next = new double[cells + 1];
                for (int n = 0; n < cells; n++) {
                    next[n]     += dist[n] * (1.0 - p);
                    next[n + 1] += dist[n] * p;
                }
                dist = next;
            }
        }

        double result = 0.0;
        for (int n = minCount; n <= cells; n++) {
            result += dist[n];
        }
        return result;
    }

    private static double matchChance(ReelWeights weights, int reel, Symbol symbol) {
        return weights.probability(reel, symbol) + weights.probability(reel, Symbol.WILD);
    }
}