package gemblast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The statistics of one simulation run. All amounts are multiples of the bet.
 *
 * Built by the nested Builder, which is fed one RoundResult at a time. The finished result is
 * read-only and can be shown as rows (for the SIM panel) or as a text report (for the console).
 */
public final class SimulationResult {

    /** Win distribution buckets (total round win, x bet): 0, under 1, 1-5, 5-20, 20-100, 100-1000, 1000+. */
    static final double[] BUCKET_LIMITS = {0, 1, 5, 20, 100, 1000};
    static final String[] BUCKET_NAMES = {"0x", "0-1x", "1-5x", "5-20x", "20-100x", "100-1000x", "1000x+"};

    /** One line of output. A row with value == null is a section heading. */
    public static final class Row {
        public final String label;
        public final String value;

        Row(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public boolean isHeading() {
            return value == null;
        }
    }

    private final List<Row> rows;
    private final String title;
    public final double rtp;
    public final double fairPrice;

    private SimulationResult(String title, List<Row> rows, double rtp, double fairPrice) {
        this.title = title;
        this.rows = rows;
        this.rtp = rtp;
        this.fairPrice = fairPrice;
    }

    public String title() {
        return title;
    }

    public List<Row> rows() {
        return rows;
    }

    /** Plain-text report for the console. */
    public String toReport() {
        StringBuilder sb = new StringBuilder("==== " + title + " ====\n");
        for (Row row : rows) {
            if (row.isHeading()) {
                sb.append('\n').append(row.label).append('\n');
            } else {
                sb.append(String.format(Locale.US, "  %-34s %s%n", row.label, row.value));
            }
        }
        return sb.toString();
    }

    // =====================================================================================
    /** Collects raw sums while the simulation runs, then turns them into rows in build(). */
    public static final class Builder {

        private final GameMode mode;
        private final long seed;

        // overall
        private long rounds;
        private double sumWin, sumWinSq, maxWin;
        private long hits;
        private final long[] buckets = new long[BUCKET_NAMES.length];

        // main game (base spin + its avalanches)
        private double sumBase;
        private long baseHits, baseSpins, baseAvalancheSteps;

        // bonus
        private final Simulator.DoubleList bonusWins = new Simulator.DoubleList();
        private final Simulator.DoubleList regularWins = new Simulator.DoubleList();
        private final Simulator.DoubleList superWins = new Simulator.DoubleList();
        private double sumBonus;
        private long bonusSpins, retriggers, sumFinalMultiplier, maxFinalMultiplier, sumStickyAtEnd;

        public Builder(GameMode mode, long seed) {
            this.mode = mode;
            this.seed = seed;
        }

        public void add(RoundResult round) {
            rounds++;
            double win = round.totalMultiplier();
            sumWin += win;
            sumWinSq += win * win;             // for the variance: Var = E[X^2] - E[X]^2
            maxWin = Math.max(maxWin, win);
            if (win > 0) {
                hits++;
            }
            buckets[bucketOf(win)]++;

            if (round.hasBaseSpin()) {
                double base = round.baseMultiplier();
                sumBase += base;
                baseSpins++;
                baseAvalancheSteps += round.baseSpin.cascades.size();
                if (base > 0) {
                    baseHits++;
                }
            }

            if (round.triggeredFreeSpins()) {
                FreeSpinsResult bonus = round.freeSpins;
                double bonusWin = round.bonusMultiplier();
                sumBonus += bonusWin;
                bonusWins.add(bonusWin);
                (bonus.superMode ? superWins : regularWins).add(bonusWin);
                bonusSpins += bonus.spinCount();
                retriggers += bonus.retriggerCount();
                sumFinalMultiplier += bonus.finalMultiplier();
                maxFinalMultiplier = Math.max(maxFinalMultiplier, bonus.finalMultiplier());
                if (bonus.superMode) {
                    sumStickyAtEnd += bonus.finalStickyWilds();
                }
            }
        }

        private static int bucketOf(double win) {
            if (win <= 0) {
                return 0;
            }
            for (int i = 1; i < BUCKET_LIMITS.length; i++) {
                if (win < BUCKET_LIMITS[i]) {
                    return i;
                }
            }
            return BUCKET_LIMITS.length;
        }

        public SimulationResult build() {
            List<Row> rows = new ArrayList<>();
            double cost = mode.costMultiplier;
            double totalBet = rounds * cost;
            double mean = sumWin / rounds;
            double variance = sumWinSq / rounds - mean * mean;
            double sd = Math.sqrt(Math.max(0, variance));
            double rtp = sumWin / totalBet;
            double rtpError = 1.96 * sd / Math.sqrt(rounds) / cost;   // 95% confidence interval
            double fairPrice = mean / Simulator.TARGET_RTP;

            heading(rows, "OVERALL (" + count(rounds) + " rounds, price " + num(cost) + "x bet)");
            row(rows, "Total bet", num(totalBet) + "x");
            row(rows, "Total win", num(sumWin) + "x");
            row(rows, "RTP", pct(rtp) + "  (+/- " + pct(rtpError) + ", 95%)");
            row(rows, "Variance (per round)", num(variance));
            row(rows, "Standard deviation (per round)", num(sd) + "x");
            row(rows, "Hit frequency (any win)", pct((double) hits / rounds) + "  (1 in " + oneIn(hits, rounds) + ")");
            row(rows, "Max win", num(maxWin) + "x");
            row(rows, "Fair price at " + pct(Simulator.TARGET_RTP), num(fairPrice) + "x bet");

            heading(rows, "MAIN GAME (base spin + avalanches)");
            if (baseSpins > 0) {
                row(rows, "RTP", pct(sumBase / totalBet));
                row(rows, "Win frequency per round", pct((double) baseHits / baseSpins));
                row(rows, "Avg avalanche steps per spin", num((double) baseAvalancheSteps / baseSpins));
            } else {
                row(rows, "RTP", "- (bought bonus, no base spin)");
            }

            heading(rows, "BONUS GAME (free spins, entire round)");
            long bonusCount = bonusWins.size();
            row(rows, "Bonus RTP", pct(sumBonus / totalBet));
            row(rows, "Bonus entry probability", pct((double) bonusCount / rounds) + "  (1 in " + oneIn(bonusCount, rounds) + ")");
            if (bonusCount > 0) {
                double[] sorted = bonusWins.sorted();
                double bonusMean = sumBonus / bonusCount;
                double bonusVar = varianceOf(sorted, bonusMean);
                row(rows, "Average win", num(bonusMean) + "x");
                row(rows, "Median win", num(median(sorted)) + "x");
                row(rows, "Variance", num(bonusVar));
                row(rows, "Standard deviation", num(Math.sqrt(bonusVar)) + "x");
                row(rows, "Max bonus win", num(sorted[sorted.length - 1]) + "x");
                row(rows, "Avg spins per bonus", num((double) bonusSpins / bonusCount));
                row(rows, "Retriggers per bonus", num((double) retriggers / bonusCount));
                row(rows, "Final multiplier (avg / max)", "x" + num((double) sumFinalMultiplier / bonusCount)
                        + " / x" + maxFinalMultiplier);
                splitRows(rows, "Regular free spins", regularWins, rounds);
                splitRows(rows, "Super free spins", superWins, rounds);
                if (superWins.size() > 0) {
                    row(rows, "Super: sticky wilds left at end", num((double) sumStickyAtEnd / superWins.size()));
                }
            }

            heading(rows, "WIN DISTRIBUTION (share of rounds)");
            for (int i = 0; i < buckets.length; i++) {
                row(rows, BUCKET_NAMES[i], pct((double) buckets[i] / rounds));
            }

            String title = mode.displayName + " - " + count(rounds) + " rounds (seed " + seed + ")";
            return new SimulationResult(title, rows, rtp, fairPrice);
        }

        private static void splitRows(List<Row> rows, String name, Simulator.DoubleList wins, long rounds) {
            if (wins.size() == 0) {
                return;
            }
            double[] sorted = wins.sorted();
            double sum = 0;
            for (double w : sorted) {
                sum += w;
            }
            row(rows, name + ": 1 in / avg / median", oneIn(wins.size(), rounds) + " / "
                    + num(sum / sorted.length) + "x / " + num(median(sorted)) + "x");
        }
    }

    // ---------------------------------------------------------------- maths helpers

    static double median(double[] sorted) {
        int n = sorted.length;
        return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
    }

    static double varianceOf(double[] values, double mean) {
        double sum = 0;
        for (double v : values) {
            sum += (v - mean) * (v - mean);
        }
        return sum / values.length;
    }

    // ---------------------------------------------------------------- formatting helpers

    private static void heading(List<Row> rows, String text) {
        rows.add(new Row(text, null));
    }

    private static void row(List<Row> rows, String label, String value) {
        rows.add(new Row(label, value));
    }

    static String pct(double fraction) {
        return String.format(Locale.US, "%.2f%%", fraction * 100.0);
    }

    static String num(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    static String count(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    static String oneIn(long hits, long total) {
        return hits == 0 ? "-" : String.format(Locale.US, "%,.0f", (double) total / hits);
    }
}