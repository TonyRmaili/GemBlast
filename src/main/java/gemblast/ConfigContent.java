package gemblast;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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
 * Every number can be typed, or stepped with the arrows next to it:
 *   <<< / >>>  -1 / +1      << / >>  -0.1 / +0.1      < / >  -0.01 / +0.01
 * Hold an arrow to repeat.
 *
 * The weights do NOT have to add up to 100: a symbol's chance is its weight divided by the total of its reel,
 * shown as a percentage next to every weight. A reel's total only has to be above 0.
 *
 * Edits stay in the text fields until APPLY & SAVE. Only then are they copied into the ValueConfig
 * (which the game and the simulator read) and written to weights.json.
 *
 * Numbers are counted in HUNDREDTHS as whole numbers (3.25 -> 325), the same trick as money in cents:
 * stepping +0.01 a hundred times with doubles would drift (0.1 + 0.2 = 0.30000000000000004).
 */
public class ConfigContent extends Table {

    /** Weights and pays allow 2 decimals: steps of 0.01. */
    private static final int DECIMALS = 2;
    private static final long MAX_HUNDREDTHS = 9_999_999;     // 99999.99, the most the field accepts
    /** Up to 5 digits, then optionally a dot and up to 2 decimals: "4", "4.5", "4.25", "4." */
    private static final Pattern NUMBER = Pattern.compile("\\d{1,5}(\\.\\d{0," + DECIMALS + "})?");
    /** The arrow steps in hundredths, outermost first: 1, 0.1, 0.01. */
    private static final long[] STEPS = {100, 10, 1};
    private static final String[] ARROWS_DOWN = {"<<<", "<<", "<"};
    private static final String[] ARROWS_UP = {">>>", ">>", ">"};

    private static final Color HEADING = new Color(1.00f, 0.85f, 0.30f, 1f);
    private static final Color DIM     = new Color(0.70f, 0.70f, 0.78f, 1f);
    private static final Color GOOD    = new Color(0.40f, 1.00f, 0.50f, 1f);
    private static final Color BAD     = new Color(1.00f, 0.40f, 0.40f, 1f);

    // Layout sizes, in one place so the columns of both tables line up.
    private static final float ICON = 36f;
    private static final float NAME_WIDTH = 190f;
    private static final float FIELD_WIDTH = 96f;
    private static final float FIELD_HEIGHT = 40f;
    private static final float ARROW_WIDTH = 28f;
    private static final float PERCENT_WIDTH = 90f;

    private static final Symbol[] SYMBOLS = Symbol.values();
    private static final int REEL1 = 0, REELS_2_TO_5 = 1;      // index into the weight arrays below

    private final ValueConfig config;
    private final TextField.TextFieldStyle normalStyle = fieldStyle(Color.WHITE);
    private final TextField.TextFieldStyle errorStyle = fieldStyle(BAD);

    private final TextField[][] weightFields = new TextField[2][SYMBOLS.length];   // [table][symbol]
    private final Label[][] percentLabels = new Label[2][SYMBOLS.length];
    private final Label[] totalLabels = new Label[2];
    private final Map<Symbol, TextField[]> payFields = new EnumMap<>(Symbol.class);
    private final Button applyButton;
    private final Label statusLabel;

    public ConfigContent(ValueConfig config) {
        this.config = config;
        top().left();
        defaults().left();

        // --- weights: Reel 1 and Reels 2-5 side by side, one row per symbol ---
        add(label("REEL WEIGHTS", 1.6f, HEADING)).padBottom(4f).row();
        add(label("Chance = weight / reel total. Arrows: <<< 1   << 0.1   < 0.01 (hold to repeat).", 1.0f, DIM))
                .padBottom(10f).row();
        add(weightTable()).row();
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

    /**
     * Columns: icon | name | Reel 1 (arrows, field, arrows) | % | gap | Reels 2-5 (arrows, field, arrows) | %
     * Icon and name are separate, fixed-width, left-aligned cells, so every row lines up.
     */
    private Table weightTable() {
        Table t = new Table();
        t.left();
        t.defaults().left().pad(3f, 3f, 3f, 3f);

        t.add().width(ICON);
        t.add().width(NAME_WIDTH);
        t.add(label("Reel 1", 1.3f, Color.WHITE)).colspan(2);
        t.add().width(24f);
        t.add(label("Reels 2-5", 1.3f, Color.WHITE)).colspan(2);
        t.row();

        for (Symbol symbol : SYMBOLS) {
            t.add(new Image(Assets.symbol(symbol))).size(ICON);
            t.add(label(symbol.displayName(), 0.9f, Color.WHITE)).width(NAME_WIDTH);
            for (int table = 0; table < 2; table++) {
                if (table == REELS_2_TO_5) {
                    t.add().width(24f);                          // gap between the two reels
                }
                TextField field = numberField();
                weightFields[table][symbol.ordinal()] = field;
                t.add(stepper(field));
                Label percent = label("", 0.9f, DIM);
                percentLabels[table][symbol.ordinal()] = percent;
                t.add(percent).width(PERCENT_WIDTH);
            }
            t.row();
        }

        // Totals under each reel's column.
        t.add().colspan(2);
        totalLabels[REEL1] = label("", 1.1f, DIM);
        t.add(totalLabels[REEL1]).colspan(2).padTop(8f);
        t.add();
        totalLabels[REELS_2_TO_5] = label("", 1.1f, DIM);
        t.add(totalLabels[REELS_2_TO_5]).colspan(2).padTop(8f);
        t.row();
        return t;
    }

    /** One row per paying symbol: 3, 4, 5 of a kind (as many columns as the JSON has values). */
    private Table payTable() {
        Table t = new Table();
        t.left();
        t.defaults().left().pad(3f, 3f, 3f, 3f);

        int columns = 0;
        for (Symbol symbol : SYMBOLS) {
            double[] pays = config.pays(symbol);
            if (pays != null) {
                columns = Math.max(columns, pays.length);
            }
        }
        t.add().width(ICON);
        t.add().width(NAME_WIDTH);
        for (int i = 0; i < columns; i++) {
            t.add(label((SlotMath.MIN_WIN_LENGTH + i) + " of a kind", 1.0f, DIM)).padLeft(3f * ARROW_WIDTH);
        }
        t.row();

        for (Symbol symbol : SYMBOLS) {
            double[] pays = config.pays(symbol);
            if (pays == null) {
                continue;                               // wild and scatter have no pays
            }
            t.add(new Image(Assets.symbol(symbol))).size(ICON);
            t.add(label(symbol.displayName(), 0.9f, Color.WHITE)).width(NAME_WIDTH);
            TextField[] fields = new TextField[pays.length];
            for (int i = 0; i < pays.length; i++) {
                fields[i] = numberField();
                t.add(stepper(fields[i])).padRight(i < pays.length - 1 ? 12f : 0f);
            }
            payFields.put(symbol, fields);
            t.row();
        }
        return t;
    }

    /** [<<<][<<][<] field [>][>>][>>>]: the arrows step the field's number by 1, 0.1 and 0.01. */
    private Table stepper(TextField field) {
        Table t = new Table();
        for (int i = 0; i < STEPS.length; i++) {                 // outermost (-1) first
            final long step = STEPS[i];
            t.add(new StepButton(ARROWS_DOWN[i], false, () -> stepField(field, -step))).size(ARROW_WIDTH, FIELD_HEIGHT)
                    .padRight(2f);
        }
        t.add(field).width(FIELD_WIDTH).height(FIELD_HEIGHT).padLeft(2f).padRight(4f);
        for (int i = STEPS.length - 1; i >= 0; i--) {            // innermost (+0.01) first, so +1 is outermost
            final long step = STEPS[i];
            t.add(new StepButton(ARROWS_UP[i], true, () -> stepField(field, step))).size(ARROW_WIDTH, FIELD_HEIGHT)
                    .padLeft(2f);
        }
        return t;
    }

    /** Adds `step` hundredths to the field's number (an invalid or empty field counts as 0), never below 0. */
    private void stepField(TextField field, long step) {
        long value = Math.max(0, parseHundredths(field.getText()));
        long next = Math.max(0, Math.min(MAX_HUNDREDTHS, value + step));
        field.setText(toText(next));
        field.setCursorPosition(field.getText().length());
        refresh();   // setText from code doesn't fire the change listener, so check by hand
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

    // ================================================================ checking (runs on every change)

    /** Re-checks every field, updates totals and percentages, enables APPLY & SAVE only when everything is valid. */
    private void refresh() {
        boolean allValid = true;

        for (int table = 0; table < 2; table++) {
            long total = 0;
            boolean tableValid = true;
            long[] values = new long[SYMBOLS.length];
            for (int i = 0; i < SYMBOLS.length; i++) {
                values[i] = parseHundredths(weightFields[table][i].getText());
                markField(weightFields[table][i], values[i] >= 0);
                if (values[i] < 0) {
                    tableValid = false;
                } else {
                    total += values[i];
                }
            }
            boolean usable = tableValid && total > 0;
            showTotal(totalLabels[table], tableValid, total);
            for (int i = 0; i < SYMBOLS.length; i++) {
                percentLabels[table][i].setText(usable && values[i] >= 0 ? percent(values[i], total) : "-");
            }
            allValid &= usable;
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

    /** "Total 92.50", or a red message when the reel can't be used. */
    private static void showTotal(Label label, boolean valid, long totalHundredths) {
        if (!valid) {
            label.setText("Fix the red fields");
            label.setColor(BAD);
        } else if (totalHundredths == 0) {
            label.setText("Total 0: add some weight");
            label.setColor(BAD);
        } else {
            label.setText("Total " + format(totalHundredths));
            label.setColor(GOOD);
        }
    }

    /** A weight's share of its reel total: 400 of 9200 hundredths -> "4.35%". */
    private static String percent(long valueHundredths, long totalHundredths) {
        return String.format(Locale.US, "%.2f%%", 100.0 * valueHundredths / totalHundredths);
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

    /** 325 -> "3.25", 400 -> "4", 450 -> "4.5" (same style as typed numbers). */
    static String toText(long hundredths) {
        return BigDecimal.valueOf(hundredths, DECIMALS).stripTrailingZeros().toPlainString();
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
        background.setLeftWidth(8f);      // inner padding so the text doesn't touch the edge
        background.setRightWidth(8f);
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

    // ================================================================ the small arrow button

    /**
     * A narrow arrow button: one step on press, then repeats while held (after a short pause).
     * Separate from Button because it needs smaller text and the hold-to-repeat.
     */
    private static final class StepButton extends Actor {
        private static final Color UP_COLOR      = new Color(0.20f, 0.45f, 0.28f, 1f);
        private static final Color DOWN_COLOR    = new Color(0.50f, 0.22f, 0.22f, 1f);
        private static final Color PRESSED_COLOR = new Color(0.35f, 0.35f, 0.45f, 1f);
        private static final float TEXT_SCALE   = 0.5f;    // relative to the font's normal scale
        private static final float REPEAT_DELAY = 0.40f;   // hold this long before repeating
        private static final float REPEAT_EVERY = 0.07f;   // then one step every 0.07 s

        private final String text;
        private final Color color;
        private final Runnable onStep;
        private final ClickListener clickListener;
        private final GlyphLayout layout = new GlyphLayout();
        private float heldFor;
        private float sinceRepeat;

        StepButton(String text, boolean up, Runnable onStep) {
            this.text = text;
            this.color = up ? UP_COLOR : DOWN_COLOR;
            this.onStep = onStep;
            clickListener = new ClickListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    onStep.run();                       // step at once on press (not on release)
                    heldFor = 0f;
                    sinceRepeat = 0f;
                    return super.touchDown(event, x, y, pointer, button);
                }
            };
            addListener(clickListener);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            if (!clickListener.isPressed()) {
                return;
            }
            heldFor += delta;
            if (heldFor < REPEAT_DELAY) {
                return;
            }
            sinceRepeat += delta;
            while (sinceRepeat >= REPEAT_EVERY) {
                sinceRepeat -= REPEAT_EVERY;
                onStep.run();
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Color bg = clickListener.isPressed() ? PRESSED_COLOR : color;
            batch.setColor(bg.r, bg.g, bg.b, bg.a * parentAlpha);
            batch.draw(Assets.white, getX(), getY(), getWidth(), getHeight());

            // Draw smaller text: scale the shared font down for this one draw, then put it back.
            BitmapFont font = Assets.font;
            float scaleX = font.getData().scaleX, scaleY = font.getData().scaleY;
            font.getData().setScale(scaleX * TEXT_SCALE, scaleY * TEXT_SCALE);
            font.setColor(1f, 1f, 1f, parentAlpha);
            layout.setText(font, text);
            font.draw(batch, layout,
                    getX() + (getWidth() - layout.width) / 2f,
                    getY() + (getHeight() + layout.height) / 2f);
            font.getData().setScale(scaleX, scaleY);
        }
    }
}