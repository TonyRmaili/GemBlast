package gemblast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The statistics of one simulation run. All amounts are multiples of the bet.
 *
 * The nested Builder collects RAW data while the simulation runs (counts, sums, sums of squares,
 * every bonus win). build() turns it into statistics with the formulas in SlotMath.
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

    private SimulationResult(String title, List<Row> rows) {
        this.title = title;
        this.rows = rows;
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
    /** Collects raw data while the simulation runs, then turns it into rows in build(). */
    public static final class Builder {

        private final SlotMath math;
        private final GameMode mode;
        private final double price;
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
        private double sumBonus, sumBonusSq;
        private long bonusSpins, retriggers, sumFinalMultiplier, maxFinalMultiplier, sumStickyAtEnd;

        public Builder(SlotMath math, GameMode mode, double price, long seed) {
            this.math = math;
            this.mode = mode;
            this.price = price;
            this.seed = seed;
        }

        public void add(RoundResult round) {
            rounds++;
            double win = round.totalMultiplier();
            sumWin += win;
            sumWinSq += win * win;
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
                sumBonusSq += bonusWin * bonusWin;
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
            double totalBet = rounds * price;
            double variance = math.variance(rounds, sumWin, sumWinSq);
            double sd = math.standardDeviation(variance);
            double rtp = math.rtp(sumWin, totalBet);
            double margin = math.rtpMargin95(rounds, sd, price);
            double fairPrice = math.fairPrice(sumWin / rounds);

            heading(rows, "OVERALL (" + count(rounds) + " rounds, price " + num(price) + "x bet)");
            row(rows, "Total bet", num(totalBet) + "x");
            row(rows, "Total win", num(sumWin) + "x");
            row(rows, "RTP", pct(rtp) + "  (+/- " + pct(margin) + ", 95%)");
            row(rows, "Variance (per round)", num(variance));
            row(rows, "Standard deviation (per round)", num(sd) + "x");
            row(rows, "Hit frequency (any win)", pct((double) hits / rounds) + "  (1 in " + oneIn(hits, rounds) + ")");
            row(rows, "Max win", num(maxWin) + "x");
            row(rows, "Fair price at " + pct(SlotMath.TARGET_RTP), num(fairPrice) + "x bet");

            heading(rows, "MAIN GAME (base spin + avalanches)");
            if (baseSpins > 0) {
                row(rows, "RTP", pct(math.rtp(sumBase, totalBet)));
                row(rows, "Win frequency per round", pct((double) baseHits / baseSpins));
                row(rows, "Avg avalanche steps per spin", num((double) baseAvalancheSteps / baseSpins));
            } else {
                row(rows, "RTP", "- (bought bonus, no base spin)");
            }

            heading(rows, "BONUS GAME (free spins, entire round)");
            long bonusCount = bonusWins.size();
            row(rows, "Bonus RTP", pct(math.rtp(sumBonus, totalBet)));
            row(rows, "Bonus entry probability", pct((double) bonusCount / rounds) + "  (1 in " + oneIn(bonusCount, rounds) + ")");
            if (bonusCount > 0) {
                double[] sorted = bonusWins.sorted();
                double bonusVar = math.variance(bonusCount, sumBonus, sumBonusSq);
                row(rows, "Average win", num(sumBonus / bonusCount) + "x");
                row(rows, "Median win", num(math.median(sorted)) + "x");
                row(rows, "Variance", num(bonusVar));
                row(rows, "Standard deviation", num(math.standardDeviation(bonusVar)) + "x");
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
            return new SimulationResult(title, rows);
        }

        private void splitRows(List<Row> rows, String name, Simulator.DoubleList wins, long rounds) {
            if (wins.size() == 0) {
                return;
            }
            double[] sorted = wins.sorted();
            double sum = 0;
            for (double w : sorted) {
                sum += w;
            }
            row(rows, name + ": 1 in / avg / median", oneIn(wins.size(), rounds) + " / "
                    + num(sum / sorted.length) + "x / " + num(math.median(sorted)) + "x");
        }
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
