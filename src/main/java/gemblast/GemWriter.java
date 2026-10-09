package gemblast;

import com.google.gson.Gson;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Writes everything for one simulation run into its own folder:
 *   output/2026-10-10_14-03-22_NORMAL_10M_rtp96.02_1a2b3c/
 *
 * Only decides WHERE (the folder name) and runs the writers;
 * DataWriter and ChartWriter decide what goes into their files.
 */
public class GemWriter {

    private static final Path OUTPUT_FOLDER = Path.of("output");

    private final Path runFolder;
    private final DataWriter dataWriter;
    private final ChartWriter chartWriter;

    public GemWriter(SimulationResult result, ValueConfig config) {
        Map<String, Map<String, Object>> data = result.data();
        String mode = (String) data.get("run").get("mode");
        long rounds = (Long) data.get("run").get("rounds");
        double rtp = (Double) data.get("overall").get("rtp");

        this.runFolder = OUTPUT_FOLDER.resolve(generateName(mode, rounds, rtp, config));

        this.dataWriter = new DataWriter(result, config, runFolder);
        this.chartWriter = new ChartWriter(result, config, runFolder);
    }

    /** Creates the run folder and runs every writer. Returns false if anything failed. */
    public boolean writeAll() {
        try {
            Files.createDirectories(runFolder);      // also creates "output" if it is missing
            dataWriter.write();        // result.csv, result.json, result.txt
            chartWriter.write();       // the PNGs
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Path runFolder() {
        return runFolder;
    }

    // ---------------------------------------------------------------- folder name

    static String generateName(String mode, long rounds, double rtp, ValueConfig config) {
        String tag = new Gson().toJson(config);
        String hashTag = Integer.toHexString(tag.hashCode());
        String rtpText = String.format(Locale.US, "%.2f", rtp * 100);   // 0.961234 -> "96.12"
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        return time + "_" + mode + "_" + shortCount(rounds) + "_rtp" + rtpText + "_" + hashTag;
    }

    static String shortCount(long n) {
        if (n >= 1_000_000_000L) {
            return oneDecimal(n / 1_000_000_000.0) + "B";
        }
        if (n >= 1_000_000) {
            return oneDecimal(n / 1_000_000.0) + "M";
        }
        if (n >= 1_000) {
            return oneDecimal(n / 1_000.0) + "k";
        }
        return Long.toString(n);
    }

    private static String oneDecimal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.DOWN)   // cut after 1 decimal
                .stripTrailingZeros()             // 10.0 -> 10
                .toPlainString();                 // 1E+1 -> "10"
    }
}