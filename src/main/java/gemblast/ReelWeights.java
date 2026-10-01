package gemblast;

import java.util.Random;

/**
 * The probability model: how likely each symbol is to land on each reel.
 *
 * Every cell is drawn independently from its reel's weight table.
 * P(symbol) = weight / total weight of that reel.
 *
 * This class knows nothing about graphics. That is on purpose: the
 * simulator (10M rounds) will use this exact class without opening a window.
 */
public final class ReelWeights {

    private static final Symbol[] SYMBOLS = Symbol.values();

    private final int[][] weights;  // weights[reel][symbol.ordinal()]
    private final int[] totals;     // sum of weights per reel

    public ReelWeights(int[][] weights) {
        this.weights = weights;
        this.totals = new int[weights.length];

        for (int reel = 0; reel < weights.length; reel++) {
            if (weights[reel].length != SYMBOLS.length) {
                throw new IllegalArgumentException("Reel " + reel + " has " + weights[reel].length
                        + " weights, expected " + SYMBOLS.length + " (one per Symbol)");
            }
            int sum = 0;
            for (int w : weights[reel]) {
                sum += w;
            }
            totals[reel] = sum;
        }
    }

    /**
     * Draws one random symbol for the given reel.
     *
     * How it works: imagine all weights laid end to end on a line of length
     * `total`. Pick a random point on that line and see whose segment it hits.
     * Example with weights [4, 6, ...]: roll 0-3 -> BLACK_DIAMOND, 4-9 -> DIAMOND, ...
     */
    public Symbol draw(int reel, Random rng) {
        int roll = rng.nextInt(totals[reel]);   // 0 .. total-1, all equally likely
        for (Symbol symbol : SYMBOLS) {
            roll -= weights[reel][symbol.ordinal()];
            if (roll < 0) {
                return symbol;
            }
        }
        throw new IllegalStateException("Unreachable: weights are broken");
    }

    /** Exact probability of one cell on this reel showing the symbol. */
    public double probability(int reel, Symbol symbol) {
        return (double) weights[reel][symbol.ordinal()] / totals[reel];
    }

    public int reelCount() {
        return weights.length;
    }

    /** Base game table (placeholder numbers, to be tuned for RTP later). */
    public static ReelWeights baseGame() {
        //                 BD  D  R  O  Sa  Em  To  Qu  W  Sc
        int[] reel1     = { 4, 6, 8, 10, 15, 17, 18, 19, 0, 3 };  // no wild on reel 1
        int[] reels2to5 = { 4, 6, 8, 10, 14, 15, 16, 19, 5, 3 };

        return new ReelWeights(new int[][] {
                reel1,
                reels2to5,
                reels2to5,
                reels2to5,
                reels2to5
        });
    }
}