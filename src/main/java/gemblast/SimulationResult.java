package gemblast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The statistics of one simulation run. All amounts are multiples of the bet.

 * The nested Builder collects RAW data while the simulation runs (counts, sums, sums of squares,
 * every bonus win). build() turns it into statistics with the formulas in SlotMath.

 * Two views of the same statistics:
 *   rows()  formatted text for the screen ("85.34%  (+/- 0.70%, 95%)")
 *   data()  raw numbers for files, by section: {"overall": {"rtp": 0.8534, ...}, "bonusGame": {...}, ...}
 */
public final class SimulationResult {

    /** Win distribution buckets (total round win, x bet): 0, under 1, 1-5, 5-20, 20-100, 100-1000, 1000+. */
    static final double[] BUCKET_LIMITS = {0, 1, 5, 20, 100, 1000};
    static final String[] BUCKET_NAMES = {"0x", "0-1x", "1-5x", "5-20x", "20-100x", "100-1000x", "1000x+"};
    private final long[] winHistogram;
    private final long zeroWins;


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
    private final Map<String, Map<String, Object>> data;
    private final String title;

    private SimulationResult(String title, List<Row> rows, Map<String, Map<String, Object>> data,
                             long[] winHistogram,long zeroWins) {
        this.data = data;
        this.title = title;
        this.rows = rows;
        this.winHistogram = winHistogram;
        this.zeroWins = zeroWins;
    }

    public long[] winHistogram(){
        return winHistogram;
    }

    public long zeroWins(){
        return zeroWins;
    }

    public String title() {
        return title;
    }

    /** The raw numbers, by section (insertion order = report order). For the CSV / JSON files. */
    public Map<String, Map<String, Object>> data() {
        return data;
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

        // graphs
        private final long[] winHistogram;
        private long zeroWins;

        public Builder(SlotMath math, GameMode mode, double price, long seed) {
            this.math = math;
            this.mode = mode;
            this.price = price;
            this.seed = seed;
            this.winHistogram = new long[math.histBins];
        }

        public void add(RoundResult round) {
            rounds++;
            double win = round.totalMultiplier();
            sumWin += win;
            sumWinSq += win * win;
            maxWin = Math.max(maxWin, win);
            if (win > 0) {
                hits++;
                winHistogram[math.histogramBin(win)]++;
            }
            else{
                zeroWins++;
            }
            buckets[math.distributionBucket(win, BUCKET_LIMITS)]++;

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

        public SimulationResult build() {
            List<Row> rows = new ArrayList<>();
            Map<String, Map<String, Object>> data = new LinkedHashMap<>();   // raw numbers for the files

            double totalBet = math.totalBet(rounds, price);
            double variance = math.variance(rounds, sumWin, sumWinSq);
            double sd = math.standardDeviation(variance);
            double rtp = math.rtp(sumWin, totalBet);
            double margin = math.rtpMargin95(rounds, sd, price);
            double averageWin = math.average(sumWin, rounds);
            double fairPrice = math.fairPrice(averageWin);
            double hitFrequency = math.frequency(hits, rounds);

            // --- the run itself: everything needed to understand (and repeat) this file ---
            Map<String, Object> run = section(data, "run");
            run.put("mode", mode.name());
            run.put("rounds", rounds);
            run.put("seed", seed);
            run.put("price", price);
            run.put("targetRtp", SlotMath.TARGET_RTP);

            // --- overall ---
            heading(rows, "OVERALL (" + count(rounds) + " rounds, price " + num(price) + "x bet)");
            row(rows, "Total bet", num(totalBet) + "x");
            row(rows, "Total win", num(sumWin) + "x");
            row(rows, "RTP", pct(rtp) + "  (+/- " + pct(margin) + ", 95%)");
            row(rows, "Variance (per round)", num(variance));
            row(rows, "Standard deviation (per round)", num(sd) + "x");
            row(rows, "Hit frequency (any win)", pct(hitFrequency) + "  (1 in " + oneIn(hits, rounds) + ")");
            row(rows, "Max win", num(maxWin) + "x");
            row(rows, "Fair price at " + pct(SlotMath.TARGET_RTP), num(fairPrice) + "x bet");

            Map<String, Object> overall = section(data, "overall");
            overall.put("totalBet", totalBet);
            overall.put("totalWin", sumWin);
            overall.put("rtp", rtp);
            overall.put("rtpMargin95", margin);
            overall.put("variance", variance);
            overall.put("standardDeviation", sd);
            overall.put("averageWin", averageWin);
            overall.put("hits", hits);
            overall.put("hitFrequency", hitFrequency);
            if (hits > 0) {
                overall.put("hitOneIn", math.oneIn(hits, rounds));
            }
            overall.put("maxWin", maxWin);
            overall.put("fairPrice", fairPrice);

            // --- main game ---
            heading(rows, "MAIN GAME (base spin + avalanches)");
            Map<String, Object> mainGame = section(data, "mainGame");
            mainGame.put("baseSpins", baseSpins);                // 0 = bought bonus, no base game
            if (baseSpins > 0) {
                double baseRtp = math.rtp(sumBase, totalBet);
                double baseWinFrequency = math.frequency(baseHits, baseSpins);
                double avgAvalancheSteps = math.average(baseAvalancheSteps, baseSpins);
                row(rows, "RTP", pct(baseRtp));
                row(rows, "Win frequency per round", pct(baseWinFrequency));
                row(rows, "Avg avalanche steps per spin", num(avgAvalancheSteps));
                mainGame.put("rtp", baseRtp);
                mainGame.put("winFrequency", baseWinFrequency);
                mainGame.put("avgAvalancheSteps", avgAvalancheSteps);
            } else {
                row(rows, "RTP", "- (bought bonus, no base spin)");
            }

            // --- bonus game ---
            heading(rows, "BONUS GAME (free spins, entire round)");
            long bonusCount = bonusWins.size();
            double bonusRtp = math.rtp(sumBonus, totalBet);
            double bonusEntry = math.frequency(bonusCount, rounds);
            row(rows, "Bonus RTP", pct(bonusRtp));
            row(rows, "Bonus entry probability", pct(bonusEntry) + "  (1 in " + oneIn(bonusCount, rounds) + ")");

            Map<String, Object> bonusGame = section(data, "bonusGame");
            bonusGame.put("rtp", bonusRtp);
            bonusGame.put("bonusCount", bonusCount);
            bonusGame.put("entryProbability", bonusEntry);
            if (bonusCount > 0) {
                double[] sorted = bonusWins.sorted();
                double bonusVar = math.variance(bonusCount, sumBonus, sumBonusSq);
                double bonusAverage = math.average(sumBonus, bonusCount);
                double bonusMedian = math.median(sorted);
                double bonusSd = math.standardDeviation(bonusVar);
                double bonusMax = sorted[sorted.length - 1];
                double avgSpins = math.average(bonusSpins, bonusCount);
                double avgRetriggers = math.average(retriggers, bonusCount);
                double avgFinalMultiplier = math.average(sumFinalMultiplier, bonusCount);

                row(rows, "Average win", num(bonusAverage) + "x");
                row(rows, "Median win", num(bonusMedian) + "x");
                row(rows, "Variance", num(bonusVar));
                row(rows, "Standard deviation", num(bonusSd) + "x");
                row(rows, "Max bonus win", num(bonusMax) + "x");
                row(rows, "Avg spins per bonus", num(avgSpins));
                row(rows, "Retriggers per bonus", num(avgRetriggers));
                row(rows, "Final multiplier (avg / max)", "x" + num(avgFinalMultiplier) + " / x" + maxFinalMultiplier);

                bonusGame.put("entryOneIn", math.oneIn(bonusCount, rounds));
                bonusGame.put("averageWin", bonusAverage);
                bonusGame.put("medianWin", bonusMedian);
                bonusGame.put("variance", bonusVar);
                bonusGame.put("standardDeviation", bonusSd);
                bonusGame.put("maxWin", bonusMax);
                bonusGame.put("avgSpins", avgSpins);
                bonusGame.put("avgRetriggers", avgRetriggers);
                bonusGame.put("avgFinalMultiplier", avgFinalMultiplier);
                bonusGame.put("maxFinalMultiplier", maxFinalMultiplier);

                splitRows(rows, bonusGame, "regular", "Regular free spins", regularWins, rounds);
                splitRows(rows, bonusGame, "super", "Super free spins", superWins, rounds);
                if (superWins.size() > 0) {
                    double avgSticky = math.average(sumStickyAtEnd, superWins.size());
                    row(rows, "Super: sticky wilds left at end", num(avgSticky));
                    bonusGame.put("superAvgStickyWildsAtEnd", avgSticky);
                }
            }

            // --- win distribution: one entry per bucket, keyed by its name ("0x", "0-1x", ...) ---
            heading(rows, "WIN DISTRIBUTION (share of rounds)");
            Map<String, Object> winDistribution = section(data, "winDistribution");
            for (int i = 0; i < buckets.length; i++) {
                double share = math.frequency(buckets[i], rounds);
                row(rows, BUCKET_NAMES[i], pct(share));
                winDistribution.put(BUCKET_NAMES[i], share);
            }

            String title = mode.displayName + " - " + count(rounds) + " rounds (seed " + seed + ")";
            return new SimulationResult(title, rows, data, winHistogram, zeroWins);
        }

        /** Adds an empty section to the data, in order, and returns it so it can be filled. */
        private static Map<String, Object> section(Map<String, Map<String, Object>> data, String name) {
            Map<String, Object> section = new LinkedHashMap<>();
            data.put(name, section);
            return section;
        }

        /** "1 in X" as text, or "-" when it never happened. */
        private String oneIn(long count, long total) {
            return count == 0 ? "-" : String.format(Locale.US, "%,.0f", math.oneIn(count, total));
        }

        /** One line for regular or super free spins, plus its raw numbers (keys start with `key`). */
        private void splitRows(List<Row> rows, Map<String, Object> section, String key, String name,
                               Simulator.DoubleList wins, long rounds) {
            if (wins.size() == 0) {
                return;
            }
            double[] sorted = wins.sorted();
            double sum = 0;
            for (double w : sorted) {
                sum += w;
            }
            double average = math.average(sum, sorted.length);
            double median = math.median(sorted);
            row(rows, name + ": 1 in / avg / median", oneIn(sorted.length, rounds) + " / "
                    + num(average) + "x / " + num(median) + "x");
            section.put(key + "Count", (long) sorted.length);
            section.put(key + "OneIn", math.oneIn(sorted.length, rounds));
            section.put(key + "AverageWin", average);
            section.put(key + "MedianWin", median);
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
}