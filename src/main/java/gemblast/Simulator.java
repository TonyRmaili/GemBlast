package gemblast;

import java.util.Arrays;
import java.util.Random;

/**
 * Monte Carlo simulator: plays many rounds with the REAL GameEngine and collects statistics.
 * Pure Java (no libGDX), so it can run inside the game (SIM panel) or on its own via main().
 *
 * All wins are measured as multiples of the bet (1.0 = one bet), so results don't depend on the currency.
 */
public final class Simulator {

    public static final double TARGET_RTP = 0.96;

    /** Called regularly while running. Return false to cancel. */
    public interface Progress {
        boolean onProgress(long done, long total);
    }

    private final ReelWeights weights;
    private final Paytable paytable;
    private final int reels;
    private final int rows;

    public Simulator(ReelWeights weights, Paytable paytable, int reels, int rows) {
        this.weights = weights;
        this.paytable = paytable;
        this.reels = reels;
        this.rows = rows;
    }

    /**
     * Plays `rounds` rounds in the given mode and returns the statistics.
     * Uses its OWN GameEngine (and so its own Board), so it never interferes with the game on screen,
     * even when it runs on a background thread.
     */
    public SimulationResult run(GameMode mode, long rounds, long seed, Progress progress) {
        GameEngine engine = new GameEngine(weights, paytable, reels, rows);
        Random rng = new Random(seed);
        SimulationResult.Builder stats = new SimulationResult.Builder(mode, seed);

        long reportEvery = Math.max(1, rounds / 200);   // ~200 progress updates, not millions
        for (long i = 0; i < rounds; i++) {
            stats.add(engine.playRound(rng, mode));
            if (progress != null && (i + 1) % reportEvery == 0) {
                if (!progress.onProgress(i + 1, rounds)) {
                    break;                                // cancelled: report what we have so far
                }
            }
        }
        return stats.build();
    }

    /**
     * Run from your IDE (right-click -> Run 'Simulator.main()') to print a full report for every mode.
     * Optional argument: number of rounds for the normal-play modes (default 10,000,000).
     * Feature buys run 10x fewer rounds: every one of them is a whole bonus, so they're much slower per round.
     */
    public static void main(String[] args) {
        long rounds = args.length > 0 ? Long.parseLong(args[0]) : 10_000_000L;
        Simulator simulator = new Simulator(ReelWeights.baseGame(), Paytable.baseGame(), 5, 3);

        for (GameMode mode : GameMode.values()) {
            long n = mode.isBuy() ? Math.max(1, rounds / 10) : rounds;
            long start = System.currentTimeMillis();
            SimulationResult result = simulator.run(mode, n, 12345L, null);
            System.out.println(result.toReport());
            System.out.printf("(%.1f s)%n%n", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    /** A growable list of plain doubles (no boxing into Double objects: much faster and smaller). */
    static final class DoubleList {
        private double[] data = new double[1024];
        private int size = 0;

        void add(double value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, size * 2);
            }
            data[size++] = value;
        }

        int size() {
            return size;
        }

        double[] sorted() {
            double[] copy = Arrays.copyOf(data, size);
            Arrays.sort(copy);
            return copy;
        }
    }
}