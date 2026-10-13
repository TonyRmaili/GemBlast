package gemblast;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The charts of a run as PNG files. One private method per chart; write() calls them all. */
class ChartWriter {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;

    /** RTP convergence: the y-axis shows the target +/- this many percentage points (early points fall outside). */
    private static final double CONVERGENCE_Y_RANGE = 40;

    private final SimulationResult result;
    private final SlotMath math;
    private final Path runFolder;

    ChartWriter(SimulationResult result, ValueConfig config, Path runFolder) {
        this.result = result;
        this.math = new SlotMath(config);      // same config as the run, so the same bins
        this.runFolder = runFolder;
    }

    void write() throws IOException {
        writeWinHistogram();
        writeRtpContribution();
        writeExceedance();
        writeRtpConvergence();
        // more charts go here
    }

    /** Saves a chart as <runFolder>/<name>.png (XChart adds the ".png" itself). */
    private void save(XYChart chart, String name) throws IOException {
        BitmapEncoder.saveBitmap(chart, runFolder.resolve(name).toString(), BitmapEncoder.BitmapFormat.PNG);
    }

    /** All rounds of the run: every round is either a zero win or in exactly one histogram bin. */
    private long totalRounds() {
        long rounds = result.zeroWins();
        for (long count : result.winHistogram()) {
            rounds += count;
        }
        return rounds;
    }

    /** The middle of bin k on a log scale: the geometric mean of its two edges. */
    private double binMiddle(int k) {
        return Math.sqrt(math.histogramEdge(k) * math.histogramEdge(k + 1));
    }

    /** Share of rounds per log bin, on log-log axes. Zero wins can't sit on a log axis: they go in the title. */
    private void writeWinHistogram() throws IOException {
        long[] counts = result.winHistogram();

        long rounds = totalRounds();

        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        for (int k = 0; k < counts.length; k++) {
            if (counts[k] == 0) {
                continue;                              // a log axis can't show 0
            }
            x.add(binMiddle(k));
            y.add((double) counts[k] / rounds);
        }
        if (x.isEmpty()) {
            return;                                    // no wins at all: nothing to draw
        }

        double zeroShare = (double) result.zeroWins() / rounds;
        XYChart chart = new XYChartBuilder().width(WIDTH).height(HEIGHT)
                .title(String.format(Locale.US, "Win distribution  (%.1f%% of rounds win 0x, not shown)", zeroShare * 100))
                .xAxisTitle("Round win (x bet)")
                .yAxisTitle("Share of rounds")
                .build();
        chart.getStyler().setXAxisLogarithmic(true);
        chart.getStyler().setYAxisLogarithmic(true);
        chart.getStyler().setLegendVisible(false);
        chart.addSeries("wins", x, y);

        save(chart, "win_histogram");
    }

    /**
     * How much of the RTP each win size pays: (sum of wins in bin k) / total bet, in percent.
     * The values add up to the run's RTP. Linear y-axis, so the dominant win sizes stand out.
     */
    private void writeRtpContribution() throws IOException {
        double[] sums = result.winSumHistogram();
        double totalBet = (Double) result.data().get("overall").get("totalBet");

        // only draw from the first to the last bin that got any wins (no long flat line of empty bins)
        int first = -1;
        int last = -1;
        for (int k = 0; k < sums.length; k++) {
            if (sums[k] > 0) {
                if (first < 0) {
                    first = k;
                }
                last = k;
            }
        }
        if (first < 0) {
            return;                                    // no wins at all: nothing to draw
        }

        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        double total = 0;
        for (int k = first; k <= last; k++) {
            double contribution = math.rtp(sums[k], totalBet);
            total += contribution;
            x.add(binMiddle(k));
            y.add(contribution * 100);                 // as % of the bet, like the RTP
        }

        // walk up from the small wins until half the RTP is reached:
        // wins above that bin pay (about) the other half
        double running = 0;
        double halfPoint = binMiddle(last);
        for (int k = first; k <= last; k++) {
            running += math.rtp(sums[k], totalBet);
            if (running >= total / 2) {
                halfPoint = binMiddle(k);
                break;
            }
        }

        XYChart chart = new XYChartBuilder().width(WIDTH).height(HEIGHT)
                .title(String.format(Locale.US,
                        "RTP contribution by win size  (total %.2f%%, about half from wins above %.1fx)",
                        total * 100, halfPoint))
                .xAxisTitle("Round win (x bet)")
                .yAxisTitle("RTP contribution (% of bet)")
                .build();
        chart.getStyler().setXAxisLogarithmic(true);
        chart.getStyler().setLegendVisible(false);
        XYSeries series = chart.addSeries("rtp", x, y);
        series.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);   // filled: the area is the RTP

        save(chart, "rtp_contribution");
    }

    /**
     * Chance that a round wins AT LEAST x: P(win >= edge(k)), from SlotMath.exceedance.
     * Exact at every bin edge, so x is the lower edge (not the middle). Log-log axes.
     */
    private void writeExceedance() throws IOException {
        long[] counts = result.winHistogram();
        long rounds = totalRounds();
        double[] exceedance = math.exceedance(counts, rounds);

        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        for (int k = 0; k < exceedance.length; k++) {
            if (exceedance[k] == 0) {
                continue;                              // above the biggest win: a log axis can't show 0
            }
            x.add(math.histogramEdge(k));
            y.add(exceedance[k]);
        }
        if (x.isEmpty()) {
            return;                                    // no wins at all: nothing to draw
        }

        String title = "Chance to win at least X  (" + oneInText(exceedance[0], "any win")
                + ", " + oneInText(exceedance[math.histogramBin(100)], "100x+")
                + ", " + oneInText(exceedance[math.histogramBin(1000)], "1000x+") + ")";

        XYChart chart = new XYChartBuilder().width(WIDTH).height(HEIGHT)
                .title(title)
                .xAxisTitle("X = round win (x bet)")
                .yAxisTitle("P(round win >= X)")
                .build();
        chart.getStyler().setXAxisLogarithmic(true);
        chart.getStyler().setYAxisLogarithmic(true);
        chart.getStyler().setLegendVisible(false);
        chart.addSeries("exceedance", x, y);

        save(chart, "win_exceedance");
    }

    /** "1 in 2,345 100x+" for the title, or "no 100x+" when it never happened in this run. */
    private static String oneInText(double probability, String what) {
        if (probability == 0) {
            return "no " + what;
        }
        return String.format(Locale.US, "1 in %,.0f %s", 1 / probability, what);
    }

    /**
     * RTP measured so far at every checkpoint (100, 126, 158, ... rounds), inside the 95% funnel
     * TARGET_RTP +/- 1.96 * SD / sqrt(n) / price. The funnel uses the SD of the WHOLE run (the best estimate).
     * If the line settles outside the funnel on the right, the true RTP is not the target.
     */
    private void writeRtpConvergence() throws IOException {
        List<Long> checkpointRounds = result.checkpointRounds();
        List<Double> checkpointSums = result.checkpointSums();
        if (checkpointRounds.isEmpty()) {
            return;                                    // no rounds: nothing to draw
        }
        double price = (Double) result.data().get("run").get("price");
        double sd = (Double) result.data().get("overall").get("standardDeviation");
        double target = SlotMath.TARGET_RTP * 100;

        List<Double> x = new ArrayList<>();
        List<Double> measured = new ArrayList<>();
        List<Double> upper = new ArrayList<>();
        List<Double> lower = new ArrayList<>();
        List<Double> targetLine = new ArrayList<>();
        for (int i = 0; i < checkpointRounds.size(); i++) {
            long n = checkpointRounds.get(i);
            double rtp = math.rtp(checkpointSums.get(i), math.totalBet(n, price)) * 100;
            double margin = math.rtpMargin95(n, sd, price) * 100;

            x.add((double) n);
            measured.add(rtp);
            upper.add(target + margin);
            lower.add(target - margin);
            targetLine.add(target);
        }

        double finalRtp = measured.get(measured.size() - 1);
        double finalMargin = math.rtpMargin95(checkpointRounds.get(checkpointRounds.size() - 1), sd, price) * 100;
        XYChart chart = new XYChartBuilder().width(WIDTH).height(HEIGHT)
                .title(String.format(Locale.US, "RTP convergence  (final %.2f%% +/- %.2f%%, target %.2f%%)",
                        finalRtp, finalMargin, target))
                .xAxisTitle("Rounds played")
                .yAxisTitle("RTP so far (%)")
                .build();
        chart.getStyler().setXAxisLogarithmic(true);
        chart.getStyler().setYAxisMin(target - CONVERGENCE_Y_RANGE);   // early points (0%, 300%...) would
        chart.getStyler().setYAxisMax(target + CONVERGENCE_Y_RANGE);   // squash everything else flat

        XYSeries upperSeries = chart.addSeries("target + 95% margin", x, upper);
        XYSeries lowerSeries = chart.addSeries("target - 95% margin", x, lower);
        XYSeries targetSeries = chart.addSeries("target", x, targetLine);
        chart.addSeries("measured RTP", x, measured);
        for (XYSeries line : List.of(upperSeries, lowerSeries, targetSeries)) {
            line.setMarker(SeriesMarkers.NONE);
            line.setLineStyle(SeriesLines.DASH_DASH);
        }

        save(chart, "rtp_convergence");
    }
}