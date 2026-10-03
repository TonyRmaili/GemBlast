package gemblast;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The CONFIG panel: edit the weights and the paytable from weights.json.
 *
 * Edits stay in the text fields until APPLY & SAVE. Only then are they copied into the ValueConfig
 * (which the game and the simulator read) and written to weights.json. APPLY & SAVE is only possible
 * when every field is a valid number and both weight tables add up to exactly 100.
 *
 * Totals are counted in HUNDREDTHS as whole numbers (3.25 -> 325), the same trick as money in cents:
 * with doubles, 33.33 + 33.33 + 33.34 can come out as 99.99999999 and a correct table would fail the check.
 */
public class ConfigContent extends Table {

    /** Weights and pays allow 2 decimals: steps of 0.01. */
    private static final int DECIMALS = 2;
    private static final long WEIGHT_TOTAL_HUNDREDTHS = Math.round(ValueConfig.WEIGHT_TOTAL * 100);
    /** Up to 5 digits, then optionally a dot and up to 2 decimals: "4", "4.5", "4.25", "4." */
    private static final Pattern NUMBER = Pattern.compile("\\d{1,5}(\\.\\d{0," + DECIMALS + "})?");

    private static final Color HEADING = new Color(1.00f, 0.85f, 0.30f, 1f);
    private static final Color DIM     = new Color(0.70f, 0.70f, 0.78f, 1f);
    private static final Color GOOD    = new Color(0.40f, 1.00f, 0.50f, 1f);
    private static final Color WARN    = new Color(1.00f, 0.80f, 0.30f, 1f);
    private static final Color BAD     = new Color(1.00f, 0.40f, 0.40f, 1f);

    private static final Symbol[] SYMBOLS = Symbol.values();
    private static final int REEL1 = 0, REELS_2_TO_5 = 1;      // index into the weight tables below

    private final ValueConfig config;
    private final TextField.TextFieldStyle normalStyle = fieldStyle(Color.WHITE);
    private final TextField.TextFieldStyle errorStyle = fieldStyle(BAD);

    private final TextField[][] weightFields = new TextField[2][SYMBOLS.length];   // [table][symbol]
    private final Label[] totalLabels = new Label[2];
    private final Map<Symbol, TextField[]> payFields = new EnumMap<>(Symbol.class);
    private final Button applyButton;
    private final Label statusLabel;

    public ConfigContent(ValueConfig config) {
        this.config = config;
        top().left();
        defaults().left();

        // --- weights: two tables side by side ---
        add(label("REEL WEIGHTS", 1.6f, HEADING)).padBottom(4f).row();
        add(label("Each table must add up to 100. Steps of 0.01.", 1.0f, DIM)).padBottom(10f).row();
        Table weights = new Table();
        weights.top();
        weights.add(weightTable("Reel 1", REEL1)).top().padRight(40f);
        weights.add(weightTable("Reels 2-5", REELS_2_TO_5)).top();
        add(weights).row();
        add(label("Design rule: the wild weight on reel 1 is normally 0.", 1.0f, DIM)).padTop(6f).row();

        // --- paytable ---
        add(label("PAYTABLE", 1.6f, HEADING)).padTop(24f).padBottom(4f).row();
        add(label("Pay per way, as a multiple of the bet.", 1.0f, DIM)).padBottom(10f).row();
        add(payTable()).row();

        // --- buttons and status ---
        Table buttons = new Table();
        applyButton = new Button("APPLY & SAVE", 240f, 50f, this::apply);
        buttons.add(applyButton).padRight(10f);
        buttons.add(new Button("REVERT", 160f, 50f, this::revert)).padRight(16f);
        statusLabel = label("", 1.1f, DIM);
        buttons.add(statusLabel);
        add(buttons).padTop(24f).row();

        revert();   // fill every field from the config
    }

    // ================================================================ building the tables

    /** Symbol rows with one weight field each, and a total line at the bottom. */
    private Table weightTable(String title, int table) {
        Table t = new Table();
        t.defaults().left().pad(3f, 4f, 3f, 4f);
        t.add(label(title, 1.3f, Color.WHITE)).colspan(2).padBottom(6f).row();

        for (Symbol symbol : SYMBOLS) {
            TextField field = numberField();
            weightFields[table][symbol.ordinal()] = field;
            t.add(symbolName(symbol)).width(220f);
            t.add(field).width(120f).height(44f);
            t.row();
        }

        totalLabels[table] = label("", 1.1f, DIM);
        t.add(totalLabels[table]).colspan(2).padTop(8f).row();
        return t;
    }

    /** One row per paying symbol: 3, 4, 5 of a kind (as many columns as the JSON has values). */
    private Table payTable() {
        Table t = new Table();
        t.defaults().left().pad(3f, 4f, 3f, 4f);

        int columns = 0;
        for (Symbol symbol : SYMBOLS) {
            double[] pays = config.pays(symbol);
            if (pays != null) {
                columns = Math.max(columns, pays.length);
            }
        }
        t.add(label("", 1.1f, DIM)).width(220f);
        for (int i = 0; i < columns; i++) {
            t.add(label((SlotMath.MIN_WIN_LENGTH + i) + " of a kind", 1.1f, DIM)).width(130f);
        }
        t.row();

        for (Symbol symbol : SYMBOLS) {
            double[] pays = config.pays(symbol);
            if (pays == null) {
                continue;                               // wild and scatter have no pays
            }
            TextField[] fields = new TextField[pays.length];
            t.add(symbolName(symbol)).width(220f);
            for (int i = 0; i < pays.length; i++) {
                fields[i] = numberField();
                t.add(fields[i]).width(120f).height(44f);
            }
            payFields.put(symbol, fields);
            t.row();
        }
        return t;
    }

    /** Small symbol picture + its name. */
    private static Table symbolName(Symbol symbol) {
        Table t = new Table();
        t.add(new Image(Assets.symbol(symbol))).size(36f).padRight(10f);
        t.add(label(symbol.displayName(), 1.1f, Color.WHITE)).left();
        return t;
    }

    /** A text field that only accepts digits and a dot, and re-checks everything on every change. */
    private TextField numberField() {
        TextField field = new TextField("", normalStyle);
        // A lambda can stand in for TextFieldFilter: it has a single method, acceptChar(field, c).
        field.setTextFieldFilter((textField, c) -> Character.isDigit(c) || c == '.');
        field.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                refresh();
            }
        });
        return field;
    }

    // ================================================================ checking (runs on every keystroke)

    /** Re-checks every field, updates the totals and enables APPLY & SAVE only when everything is valid. */
    private void refresh() {
        boolean allValid = true;

        for (int table = 0; table < 2; table++) {
            long total = 0;
            boolean tableValid = true;
            for (TextField field : weightFields[table]) {
                long value = parseHundredths(field.getText());
                markField(field, value >= 0);
                if (value < 0) {
                    tableValid = false;
                } else {
                    total += value;
                }
            }
            boolean exact = tableValid && total == WEIGHT_TOTAL_HUNDREDTHS;
            showTotal(totalLabels[table], tableValid, total);
            allValid &= exact;
        }

        for (TextField[] fields : payFields.values()) {
            for (TextField field : fields) {
                boolean ok = parseHundredths(field.getText()) >= 0;
                markField(field, ok);
                allValid &= ok;
            }
        }

        applyButton.setEnabled(allValid);
    }

    /** "Total 97.50  -  2.50 left", "Total 101.00  -  1.00 over" or "Total 100.00  OK". */
    private static void showTotal(Label label, boolean valid, long totalHundredths) {
        if (!valid) {
            label.setText("Fix the red fields");
            label.setColor(BAD);
            return;
        }
        long difference = WEIGHT_TOTAL_HUNDREDTHS - totalHundredths;
        String total = "Total " + format(totalHundredths);
        if (difference == 0) {
            label.setText(total + "  OK");
            label.setColor(GOOD);
        } else if (difference > 0) {
            label.setText(total + "  -  " + format(difference) + " left");
            label.setColor(WARN);
        } else {
            label.setText(total + "  -  " + format(-difference) + " over");
            label.setColor(BAD);
        }
    }

    private void markField(TextField field, boolean valid) {
        TextField.TextFieldStyle wanted = valid ? normalStyle : errorStyle;
        if (field.getStyle() != wanted) {
            field.setStyle(wanted);
        }
    }

    // ================================================================ buttons

    /** Copies the fields into the config and writes weights.json. Only reachable when everything is valid. */
    private void apply() {
        config.setReel1Weights(readWeights(REEL1));
        config.setReel2to5Weights(readWeights(REELS_2_TO_5));
        for (Map.Entry<Symbol, TextField[]> entry : payFields.entrySet()) {
            TextField[] fields = entry.getValue();
            double[] pays = new double[fields.length];
            for (int i = 0; i < fields.length; i++) {
                pays[i] = parseHundredths(fields[i].getText()) / 100.0;
            }
            config.setPays(entry.getKey(), pays);
        }

        try {
            config.save();
            setStatus("Applied and saved to weights.json", GOOD);
        } catch (IllegalStateException e) {
            setStatus("Applied, but not saved: " + e.getMessage(), BAD);
        }
    }

    /** Puts the values from the config back into every field, throwing away unsaved edits. */
    private void revert() {
        for (int table = 0; table < 2; table++) {
            double[] weights = config.weights(table == REEL1 ? 0 : 1);
            for (int i = 0; i < SYMBOLS.length; i++) {
                weightFields[table][i].setText(toText(weights[i]));
            }
        }
        for (Map.Entry<Symbol, TextField[]> entry : payFields.entrySet()) {
            double[] pays = config.pays(entry.getKey());
            TextField[] fields = entry.getValue();
            for (int i = 0; i < fields.length; i++) {
                fields[i].setText(toText(pays[i]));
            }
        }
        setStatus("", DIM);
        refresh();   // setText from code doesn't fire the change listener, so check by hand
    }

    private double[] readWeights(int table) {
        double[] weights = new double[SYMBOLS.length];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = parseHundredths(weightFields[table][i].getText()) / 100.0;
        }
        return weights;
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setColor(color);
    }

    // ================================================================ number helpers

    /** "3.25" -> 325, "4" -> 400, "4." -> 400. Returns -1 for anything that isn't a valid number. */
    static long parseHundredths(String text) {
        if (!NUMBER.matcher(text).matches()) {
            return -1;
        }
        // BigDecimal is exact: movePointRight(2) turns 3.25 into exactly 325, no rounding errors.
        return new BigDecimal(text).movePointRight(DECIMALS).longValueExact();
    }

    /** 4.0 -> "4", 4.5 -> "4.5", 0.125 -> "0.13" (rounded to 2 decimals for display). */
    static String toText(double value) {
        return BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** 325 -> "3.25" */
    static String format(long hundredths) {
        return String.format(Locale.US, "%d.%02d", hundredths / 100, hundredths % 100);
    }

    // ================================================================ styles

    private static TextField.TextFieldStyle fieldStyle(Color textColor) {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = Assets.font;
        style.fontColor = textColor;
        Drawable background = Assets.solid(new Color(0.08f, 0.08f, 0.12f, 1f));
        background.setLeftWidth(10f);     // inner padding so the text doesn't touch the edge
        background.setRightWidth(10f);
        style.background = background;
        Drawable cursor = Assets.solid(Color.WHITE);
        cursor.setMinWidth(2f);
        style.cursor = cursor;
        style.selection = Assets.solid(new Color(0.25f, 0.45f, 0.85f, 1f));
        return style;
    }

    private static Label label(String text, float scale, Color color) {
        Label label = new Label(text, new Label.LabelStyle(Assets.font, color));
        label.setFontScale(scale);
        return label;
    }
}