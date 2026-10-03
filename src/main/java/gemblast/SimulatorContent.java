package gemblast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import java.util.Locale;

/**
 * Content of the SIM panel: pick a mode, type a number of rounds, press RUN, read the statistics.
 * Built once and kept, so the last results are still there when you reopen the panel.
 *
 * THREADS: a 10M-round run takes ~30 seconds. Running it on the render thread would freeze the window,
 * so it runs on a separate background thread. libGDX objects (labels, tables) may ONLY be changed on
 * the render thread, so the background thread hands every screen update back via Gdx.app.postRunnable().
 */
public class SimulatorContent extends Table {

    private static final Color HEADING = new Color(1.00f, 0.85f, 0.30f, 1f);
    private static final Color DIM     = new Color(0.70f, 0.70f, 0.78f, 1f);
    private static final Color ERROR   = new Color(1.00f, 0.40f, 0.40f, 1f);
    private static final Color ROW_BG  = new Color(0.20f, 0.20f, 0.26f, 1f);
    private static final long MAX_ROUNDS = 100_000_000L;

    private final Simulator simulator;
    private final TextField roundsField;
    private final Label modeLabel;
    private final Label statusLabel;
    private final Table resultsTable;
    private final Button runButton;
    private final Button cancelButton;

    private GameMode selectedMode = GameMode.NORMAL;
    private boolean running = false;
    // "volatile": written by the render thread (CANCEL button), read by the background thread.
    // Without it, the background thread might never see the change.
    private volatile boolean cancelRequested = false;

    public SimulatorContent(Simulator simulator) {
        this.simulator = simulator;
        top().left();
        defaults().left();

        // --- mode ---
        add(label("MODE", 1.4f, HEADING)).padBottom(6f).row();
        Table modeButtons = new Table();
        for (GameMode mode : GameMode.values()) {
            modeButtons.add(new Button(shortName(mode), 170f, 44f, () -> selectMode(mode))).padRight(8f);
        }
        add(modeButtons).row();
        modeLabel = label("", 1.2f, Color.WHITE);
        add(modeLabel).padTop(6f).row();

        // --- rounds ---
        add(label("ROUNDS", 1.4f, HEADING)).padTop(16f).padBottom(6f).row();
        roundsField = new TextField("1000000", textFieldStyle());
        roundsField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());   // numbers only
        Table roundsRow = new Table();
        roundsRow.add(roundsField).width(240f).height(50f).padRight(12f);
        roundsRow.add(new Button("10K", 90f, 44f, () -> roundsField.setText("10000"))).padRight(6f);
        roundsRow.add(new Button("100K", 90f, 44f, () -> roundsField.setText("100000"))).padRight(6f);
        roundsRow.add(new Button("1M", 90f, 44f, () -> roundsField.setText("1000000"))).padRight(6f);
        roundsRow.add(new Button("10M", 90f, 44f, () -> roundsField.setText("10000000")));
        add(roundsRow).row();

        // --- run / cancel / status ---
        Table runRow = new Table();
        runButton = new Button("RUN", 170f, 50f, this::run);
        cancelButton = new Button("CANCEL", 170f, 50f, () -> cancelRequested = true);
        cancelButton.setEnabled(false);
        runRow.add(runButton).padRight(10f);
        runRow.add(cancelButton).padRight(16f);
        statusLabel = label("Ready.", 1.2f, DIM);
        runRow.add(statusLabel);
        add(runRow).padTop(16f).row();

        // --- results ---
        resultsTable = new Table();
        resultsTable.top().left();
        add(resultsTable).growX().padTop(16f).row();

        selectMode(GameMode.NORMAL);
    }

    private void selectMode(GameMode mode) {
        if (running) {
            return;
        }
        selectedMode = mode;
        modeLabel.setText("Selected: " + mode.displayName);
    }

    private void run() {
        if (running) {
            return;
        }
        long rounds;
        try {
            rounds = Long.parseLong(roundsField.getText());
        } catch (NumberFormatException e) {   // empty field or a number too big for a long
            statusLabel.setText("Type a number of rounds.");
            return;
        }
        rounds = Math.max(1, Math.min(rounds, MAX_ROUNDS));

        running = true;
        cancelRequested = false;
        runButton.setEnabled(false);
        cancelButton.setEnabled(true);
        statusLabel.setColor(Color.WHITE);
        statusLabel.setText("Running...");

        final GameMode mode = selectedMode;
        final long total = rounds;
        final long seed = System.nanoTime();          // shown in the results, so a run can be repeated exactly
        final long start = System.currentTimeMillis();

        // Everything inside this lambda runs on the background thread.
        Thread worker = new Thread(() -> {
            try {
                SimulationResult result = simulator.run(mode, total, seed, (done, all) -> {
                    Gdx.app.postRunnable(() -> statusLabel.setText(String.format(Locale.US,
                            "Running... %.0f%%  (%,d / %,d)", 100.0 * done / all, done, all)));
                    return !cancelRequested;
                });
                double seconds = (System.currentTimeMillis() - start) / 1000.0;
                Gdx.app.postRunnable(() -> finished(result, seconds));    // back to the render thread
            } catch (RuntimeException e) {
                // e.g. a SlotMath method that isn't written yet. Without this catch the background
                // thread would die silently and the panel would say "Running..." forever.
                e.printStackTrace();
                Gdx.app.postRunnable(() -> failed(e));
            }
        }, "simulator");
        worker.setDaemon(true);   // don't keep the program alive if the window is closed mid-run
        worker.start();
    }

    /** Runs on the render thread once the simulation is done (or cancelled). */
    private void finished(SimulationResult result, double seconds) {
        running = false;
        runButton.setEnabled(true);
        cancelButton.setEnabled(false);
        statusLabel.setText((cancelRequested ? "Cancelled (partial results). " : "Done. ")
                + String.format(Locale.US, "%.1f s", seconds));
        showResults(result);
        System.out.println(result.toReport());   // also in the console, handy to copy into the write-up
    }

    /** Runs on the render thread when the simulation threw an exception. */
    private void failed(RuntimeException e) {
        running = false;
        runButton.setEnabled(true);
        cancelButton.setEnabled(false);
        statusLabel.setColor(ERROR);
        statusLabel.setText(e.getMessage());
    }

    private void showResults(SimulationResult result) {
        resultsTable.clearChildren();
        resultsTable.add(label(result.title(), 1.3f, Color.WHITE)).colspan(2).left().padBottom(6f).row();

        boolean shaded = false;
        for (SimulationResult.Row row : result.rows()) {
            if (row.isHeading()) {
                resultsTable.add(label(row.label, 1.3f, HEADING)).colspan(2).left().padTop(14f).padBottom(4f).row();
                shaded = false;
                continue;
            }
            Table line = new Table();
            if (shaded) {
                line.setBackground(Assets.solid(ROW_BG));   // alternate shading makes long lists easier to read
            }
            shaded = !shaded;
            line.add(label(row.label, 1.1f, DIM)).width(420f).left().pad(3f, 8f, 3f, 8f);
            line.add(label(row.value, 1.1f, Color.WHITE)).expandX().left().pad(3f, 8f, 3f, 8f);
            resultsTable.add(line).colspan(2).growX().row();
        }
    }

    private static String shortName(GameMode mode) {
        switch (mode) {
            case NORMAL:               return "NORMAL";
            case BUY_FREE_SPINS:       return "BUY FS";
            case BUY_SUPER_FREE_SPINS: return "BUY SUPER";
            case BOOST_SCATTER:        return "SCATTER+";
            case BOOST_WILD:           return "WILD+";
            default:                   return mode.name();
        }
    }

    private static TextField.TextFieldStyle textFieldStyle() {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = Assets.font;
        style.fontColor = Color.WHITE;
        Drawable background = Assets.solid(new Color(0.08f, 0.08f, 0.12f, 1f));
        background.setLeftWidth(12f);     // inner padding so the text doesn't touch the edge
        background.setRightWidth(12f);
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
