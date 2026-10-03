package gemblast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The tunable numbers, read from weights.json in the project root (the folder the game runs in).
 * Only data and file reading/writing; the CONFIG panel (ConfigContent) is the screen that edits it.
 *
 * The field names match the JSON keys, which is how Gson knows what goes where:
 * {
 *   "reel1":    [10 weights, Symbol order],      reel 1 (no wild there by design)
 *   "reel2to5": [10 weights, Symbol order],      reels 2, 3, 4 and 5
 *   "paytable": { "BLACK_DIAMOND": [pay for 3, 4, 5 of a kind], ... }
 * }
 * Symbol order = the order of the Symbol enum: BLACK_DIAMOND, DIAMOND, ... WILD, SCATTER.
 * Weights are percentages: each reel's weights add up to 100, with up to 2 decimals.
 */
public class ValueConfig {

    public static final Path FILE = Path.of("weights.json");
    public static final double WEIGHT_TOTAL = 100.0;

    public List<Double> reel1;
    public List<Double> reel2to5;
    public Map<String, List<Double>> paytable;

    // ---------------------------------------------------------------- file

    /** Reads weights.json. Fails with a clear message if the file is missing or incomplete. */
    public static ValueConfig load() {
        if (!Files.exists(FILE)) {
            throw new IllegalStateException("weights.json not found in " + FILE.toAbsolutePath()
                    + " (check the run configuration's working directory)");
        }
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            ValueConfig config = new Gson().fromJson(reader, ValueConfig.class);
            config.validate();
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + FILE.toAbsolutePath(), e);
        }
    }

    /** Writes the current values back to weights.json (pretty-printed so it stays readable). */
    public void save() {
        validate();
        try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(this, writer);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write " + FILE.toAbsolutePath(), e);
        }
    }

    // ---------------------------------------------------------------- reading

    /** The weights for one reel (0 = leftmost), one per symbol in Symbol order. */
    public double[] weights(int reel) {
        return toArray(reel == 0 ? reel1 : reel2to5);
    }

    /** Pays for 3, 4, 5 of a kind (x bet per way), or null if the symbol doesn't pay (wild, scatter). */
    public double[] pays(Symbol symbol) {
        List<Double> list = paytable.get(symbol.name());
        return list == null ? null : toArray(list);
    }

    // ---------------------------------------------------------------- changing (used by the CONFIG panel)

    public void setReel1Weights(double[] weights) {
        reel1 = toList(weights);
    }

    public void setReel2to5Weights(double[] weights) {
        reel2to5 = toList(weights);
    }

    public void setPays(Symbol symbol, double[] pays) {
        // LinkedHashMap keeps the insertion order, so the saved file stays in the same order.
        Map<String, List<Double>> copy = new LinkedHashMap<>(paytable);
        copy.put(symbol.name(), toList(pays));
        paytable = copy;
    }

    // ---------------------------------------------------------------- helpers

    /** Catches typos in the JSON early, instead of a strange crash in the middle of a spin. */
    private void validate() {
        int symbols = Symbol.values().length;
        if (reel1 == null || reel1.size() != symbols || reel2to5 == null || reel2to5.size() != symbols) {
            throw new IllegalStateException("weights.json: reel1 and reel2to5 need " + symbols
                    + " weights each (one per Symbol, in enum order)");
        }
        if (paytable == null) {
            throw new IllegalStateException("weights.json: \"paytable\" is missing");
        }
        for (String name : paytable.keySet()) {
            Symbol.valueOf(name);   // throws if a name is misspelled, e.g. "BLACKDIAMOND"
        }
    }

    private static double[] toArray(List<Double> list) {
        double[] result = new double[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    private static List<Double> toList(double[] values) {
        List<Double> list = new ArrayList<>();
        for (double v : values) {
            list.add(v);
        }
        return list;
    }
}