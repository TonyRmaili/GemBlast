package gemblast;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

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
        // more charts go here
    }

    /** Saves a chart as <runFolder>/<name>.png (XChart adds the ".png" itself). */
    private void save(XYChart chart, String name) throws IOException {
        BitmapEncoder.saveBitmap(chart, runFolder.resolve(name).toString(), BitmapEncoder.BitmapFormat.PNG);
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
            // the middle of the bin on a log scale (the geometric mean of its two edges)
            x.add(Math.sqrt(math.histogramEdge(k) * math.histogramEdge(k + 1)));
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
}