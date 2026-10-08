package gemblast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

public class GemWriter {

    private static final Path OUTPUT_FOLDER = Path.of("output");

    private final SimulationResult result;
    private final Map<String, Map<String,Object>> data;
    private final ValueConfig config;
    private final Path runFolder;


    public GemWriter(SimulationResult result, ValueConfig config){
        this.config = config;
        this.result = result;
        this.data = result.data();

        Map<String, Object> run = data.get("run");
        Map<String, Object> overall = data.get("overall");
        String mode = (String) run.get("mode");
        long rounds = (Long) run.get("rounds");
        double rtp = (Double) overall.get("rtp");

        this.runFolder = OUTPUT_FOLDER.resolve(generateName(mode, rounds, rtp, config));

    }

    public boolean writeAll() {
        try {
            Files.createDirectories(OUTPUT_FOLDER);
            Files.createDirectories(runFolder);

            writeCsv();
            writeJson();
            writeText();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private Path file(String extension) {
        return runFolder.resolve("result." + extension);
    }


    private void writeCsv() throws IOException {
        try (Writer writer = Files.newBufferedWriter(file("csv"), StandardCharsets.UTF_8)) {
            writer.write("section,key,value\n");

            // the results: every section, every key, in report order
            for (Map.Entry<String, Map<String, Object>> section : data.entrySet()) {
                for (Map.Entry<String, Object> entry : section.getValue().entrySet()) {
                    csvLine(writer, section.getKey(), entry.getKey(), entry.getValue());
                }
            }

            // the config used: weights per reel table, then the paytable
            for (Symbol symbol : Symbol.values()) {
                csvLine(writer, "config", "reel1." + symbol.name(), config.weights(0)[symbol.ordinal()]);
            }
            for (Symbol symbol : Symbol.values()) {
                csvLine(writer, "config", "reel2to5." + symbol.name(), config.weights(1)[symbol.ordinal()]);
            }
            for (Symbol symbol : Symbol.values()) {
                double[] pays = config.pays(symbol);
                if (pays == null) {
                    continue;                                   // wild and scatter have no pays
                }
                for (int i = 0; i < pays.length; i++) {
                    int count = SlotMath.MIN_WIN_LENGTH + i;    // 3, 4, 5 of a kind
                    csvLine(writer, "config", "paytable." + symbol.name() + "." + count, pays[i]);
                }
            }
        }
    }

    /** One "section,key,value" line. String.valueOf always writes a dot as decimal separator. */
    private static void csvLine(Writer writer, String section, String key, Object value) throws IOException {
        writer.write(section + "," + key + "," + String.valueOf(value) + "\n");
    }

    private void writeJson() throws IOException {
        Map<String, Object> output = new LinkedHashMap<>(data);
        output.put("config", config);

        try (Writer writer = Files.newBufferedWriter(file("json"), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(output, writer);
        }
    }

    private void writeText() throws IOException {
        try (Writer writer = Files.newBufferedWriter(file("txt"), StandardCharsets.UTF_8)) {
            writer.write(result.toReport());
        }
    }

    public String generateName(String mode,long rounds, double rtp,ValueConfig config){
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
