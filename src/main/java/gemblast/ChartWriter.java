package gemblast;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The charts of a run as PNG files. One private method per chart; write() calls them all. */
class ChartWriter {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;

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
        // more charts go here
    }

    /** Saves a chart as <runFolder>/<name>.png (XChart adds the ".png" itself). */
    private void save(XYChart chart, String name) throws IOException {
        BitmapEncoder.saveBitmap(chart, runFolder.resolve(name).toString(), BitmapEncoder.BitmapFormat.PNG);
    }

    /** The middle of bin k on a log scale: the geometric mean of its two edges. */
    private double binMiddle(int k) {
        return Math.sqrt(math.histogramEdge(k) * math.histogramEdge(k + 1));
    }

    /** Share of rounds per log bin, on log-log axes. Zero wins can't sit on a log axis: they go in the title. */
    private void writeWinHistogram() throws IOException {
        long[] counts = result.winHistogram();

        long rounds = result.zeroWins();            // every round is either a zero or in one bin
        for (long count : counts) {
            rounds += count;
        }

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
}