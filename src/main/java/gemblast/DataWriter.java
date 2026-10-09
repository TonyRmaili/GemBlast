package gemblast;

import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** The numbers of a run as files: result.csv, result.json and result.txt. */
class DataWriter {

    private final SimulationResult result;
    private final Map<String, Map<String, Object>> data;
    private final ValueConfig config;
    private final Path runFolder;

    DataWriter(SimulationResult result, ValueConfig config, Path runFolder) {
        this.result = result;
        this.data = result.data();
        this.config = config;
        this.runFolder = runFolder;
    }

    void write() throws IOException {
        writeCsv();
        writeJson();
        writeText();
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
}