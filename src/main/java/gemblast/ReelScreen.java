package gemblast;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import java.util.Map;

/**
 * The big game area: reels, a win banner, and a centre message panel.
 * It only REPLAYS results the GameEngine already decided; it never decides anything.
 */
public class ReelScreen extends Group {

    /**
     * What ReelScreen reports back while it replays a spin.
     * An interface is a list of methods a class promises to have; Main "implements" it.
     * (A lambda can only stand in for an interface with ONE method, so with more we use an interface.)
     * "default" methods have an empty body here, so a listener only has to write the ones it cares about.
     */
    public interface SpinListener {
        void onWinShown(long amountCents);        // a win just appeared on screen
        void onMultiplierChanged(int multiplier); // after each avalanche step
        default void onShatterMeters(int[] meters) { }   // free spins: the SHATTER meters changed
        default void onShatterExplode(Symbol gem) { }    // free spins: this gem's meter exploded
    }

    private static final Color BACKGROUND    = new Color(0.15f, 0.15f, 0.20f, 1f);
    private static final Color BACKGROUND_FS = new Color(0.22f, 0.12f, 0.30f, 1f);   // purple tint in free spins
    private static final Color BANNER_COLOR  = new Color(0.05f, 0.05f, 0.08f, 0.9f);
    private static final Color MESSAGE_COLOR = new Color(0.10f, 0.05f, 0.15f, 0.95f);

    // Timings in seconds. Tweak these to change the feel of the game.
    private static final float REVEAL_DELAY    = 0.25f;  // between each reel appearing
    private static final float WIN_SHOW_TIME   = 1.0f;   // each winning combo on screen
    private static final float CLEAR_TIME      = 0.35f;  // winning cells gone, gaps visible
    private static final float FALL_TIME       = 0.35f;  // survivors dropped down, empty spaces on top
    private static final float REFILL_TIME     = 0.45f;  // new symbols in, before checking for wins again
    private static final float BONUS_SHOW_TIME = 1.6f;   // each row BONUS on screen (a bit longer: it's special)
    private static final float SHATTER_SHOW_TIME = 1.4f; // each SHATTER explosion on screen

    private final Reel[] reels;
    private final int rowCount;
    private final Table winBanner;
    private final Label winLabel;
    private final Table messagePanel;
    private final Label messageLabel;
    private Color background = BACKGROUND;

    /**
     * @param maxWidth  / maxHeight  the space we are allowed to use; cells stay square,
     *                  so the final size is the largest grid that fits inside.
     */
    public ReelScreen(int reelCount, int rowCount, float maxWidth, float maxHeight, float padding) {
        setTransform(false);
        this.rowCount = rowCount;

        float cellFromWidth  = (maxWidth  - padding * (reelCount + 1)) / reelCount;
        float cellFromHeight = (maxHeight - padding * (rowCount + 1)) / rowCount;
        float cellSize = Math.min(cellFromWidth, cellFromHeight);

        setSize(reelCount * cellSize + (reelCount + 1) * padding,
                rowCount  * cellSize + (rowCount  + 1) * padding);

        reels = new Reel[reelCount];
        for (int i = 0; i < reelCount; i++) {
            Reel reel = new Reel(rowCount, cellSize, padding);
            reel.setPosition(padding + i * (cellSize + padding), padding);
            reels[i] = reel;
            addActor(reel);
        }

        // Win banner ("Ruby x4  6 ways  1.80"), straddling the bottom edge of the grid.
        winLabel = new Label("", new Label.LabelStyle(Assets.font, Color.WHITE));
        winLabel.setFontScale(1.4f);
        winBanner = new Table();
        winBanner.setBackground(Assets.solid(BANNER_COLOR));
        winBanner.add(winLabel).pad(8f, 24f, 8f, 24f);
        winBanner.setVisible(false);
        addActor(winBanner);

        // Centre message ("FREE SPINS!"). Added last so it's drawn above everything.
        messageLabel = new Label("", new Label.LabelStyle(Assets.font, new Color(1f, 0.85f, 0.3f, 1f)));
        messageLabel.setFontScale(2.4f);
        messageLabel.setAlignment(Align.center);
        messagePanel = new Table();
        messagePanel.setBackground(Assets.solid(MESSAGE_COLOR));
        messagePanel.add(messageLabel).pad(24f, 48f, 24f, 48f);
        messagePanel.setVisible(false);
        addActor(messagePanel);
    }

    /** Empties the grid. Doesn't cancel running animations (Main's reset does that with clearActions()). */
    public void setEmpty() {
        for (Reel reel : reels) {
            reel.setEmpty();
        }
        winBanner.setVisible(false);
        messagePanel.setVisible(false);
    }

    public void setFreeSpinsMode(boolean freeSpins) {
        background = freeSpins ? BACKGROUND_FS : BACKGROUND;
    }

    /**
     * Replays one spin as a sequence of timed steps:
     *   1. reveal the first grid reel by reel
     *   2. for each avalanche step: show each win -> clear the winning cells
     *      -> survivors fall -> new symbols drop in
     *   3. (super free spins) each row BONUS: the row lights up and pays
     *   4. (free spins) each SHATTER explosion: the gem's meter explodes and pays
     *   5. onDone
     */
    public void playSpin(SpinResult spin, long betCents, SpinListener listener, Runnable onDone) {
        setEmpty();
        SequenceAction sequence = Actions.sequence();

        // SHATTER meters as they should look after each step, worked out now from the spin's data.
        final int[] meters = spin.hasShatterMeters() ? spin.shatterMetersBefore.clone() : null;
        if (meters != null) {
            final int[] before = meters.clone();
            sequence.addAction(Actions.run(() -> listener.onShatterMeters(before)));
        }

        // 1) reveal, left to right. Sticky wilds from earlier spins are already there before the reveal.
        sequence.addAction(Actions.run(() -> showStickyWilds(spin.stickyBefore, spin.initialGrid)));
        for (int i = 0; i < reels.length; i++) {
            final Reel reel = reels[i];
            final Symbol[] symbols = spin.initialGrid[i];
            sequence.addAction(Actions.delay(REVEAL_DELAY));
            sequence.addAction(Actions.run(() -> reel.showSymbols(symbols)));
        }
        sequence.addAction(Actions.run(() -> showStickyWilds(spin.stickyLanded, null)));   // new wilds lock

        // 2) avalanche steps
        for (SpinResult.Cascade cascade : spin.cascades) {
            final int[] metersAfterStep;
            if (meters != null) {
                for (Map.Entry<Symbol, Integer> entry : cascade.shattered.entrySet()) {
                    meters[entry.getKey().ordinal()] += entry.getValue();
                }
                metersAfterStep = meters.clone();
            } else {
                metersAfterStep = null;
            }

            for (Win win : cascade.wins) {
                sequence.addAction(Actions.run(() -> {
                    highlight(win);
                    showBanner(bannerText(win, cascade, betCents));
                    listener.onWinShown(cascade.amount(win, betCents));
                }));
                sequence.addAction(Actions.delay(WIN_SHOW_TIME));
            }

            sequence.addAction(Actions.run(() -> {
                clearHighlights();
                winBanner.setVisible(false);
                clearCells(cascade.removed);
                listener.onMultiplierChanged(cascade.multiplierAfter);
                if (metersAfterStep != null) {
                    listener.onShatterMeters(metersAfterStep);   // the shattered gems fly into the meters
                }
            }));
            sequence.addAction(Actions.delay(CLEAR_TIME));

            sequence.addAction(Actions.run(() -> showFallen(cascade.gridAfter, cascade.newCells)));
            sequence.addAction(Actions.delay(FALL_TIME));

            sequence.addAction(Actions.run(() -> {
                showGrid(cascade.gridAfter);
                showStickyWilds(cascade.stickyAfter, null);
            }));
            sequence.addAction(Actions.delay(REFILL_TIME));
        }

        // 3) row BONUS (super free spins only): every paying row lights up on its own, after all avalanches
        for (int row = 0; row < spin.rowCount(); row++) {
            if (!spin.hasRowBonus(row)) {
                continue;
            }
            final int bonusRow = row;
            sequence.addAction(Actions.run(() -> {
                highlightRow(bonusRow);
                showBanner(rowBonusText(spin, bonusRow, betCents));
                listener.onWinShown(spin.rowBonusAmount(bonusRow, betCents));
            }));
            sequence.addAction(Actions.delay(BONUS_SHOW_TIME));
        }
        if (spin.hasAnyRowBonus()) {
            sequence.addAction(Actions.run(() -> {
                clearHighlights();
                winBanner.setVisible(false);
            }));
        }

        // 4) SHATTER explosions (free spins): every full meter explodes, with the end-of-spin multiplier
        for (int i = 0; i < spin.shatterCount(); i++) {
            final int index = i;
            final Symbol gem = spin.shatterGem(i);
            meters[gem.ordinal()] -= SlotMath.SHATTER_METER_SIZE;
            final int[] metersAfterExplosion = meters.clone();
            sequence.addAction(Actions.run(() -> {
                listener.onShatterExplode(gem);
                listener.onShatterMeters(metersAfterExplosion);
                showBanner(shatterText(spin, index, betCents));
                listener.onWinShown(spin.shatterAmount(index, betCents));
            }));
            sequence.addAction(Actions.delay(SHATTER_SHOW_TIME));
        }
        if (spin.shatterCount() > 0) {
            sequence.addAction(Actions.run(() -> winBanner.setVisible(false)));
        }

        // 5) done
        sequence.addAction(Actions.run(onDone));
        addAction(sequence);   // the Stage calls act() every frame, which advances the sequence
    }

    /** Big centred message for a few seconds, then onDone. "\n" in the text starts a new line. */
    public void showMessage(String text, float seconds, Runnable onDone) {
        messageLabel.setText(text);
        messagePanel.pack();
        messagePanel.setPosition(getWidth() / 2f, getHeight() / 2f, Align.center);
        messagePanel.setVisible(true);
        addAction(Actions.sequence(
                Actions.delay(seconds),
                Actions.run(() -> {
                    messagePanel.setVisible(false);
                    onDone.run();
                })));
    }

    /** Gold frame on every cell showing `symbol` in `grid`, everything else dimmed (e.g. the scatters). */
    public void highlightSymbol(Symbol symbol, Symbol[][] grid) {
        for (int reel = 0; reel < reels.length; reel++) {
            for (int row = 0; row < rowCount; row++) {
                boolean match = grid[reel][row] == symbol;
                reels[reel].getCell(row).setLook(match ? Cell.Look.WIN : Cell.Look.DIMMED);
            }
        }
    }

    /**
     * Cyan frames on the sticky wilds (super free spins). An all-false mask removes them.
     * If grid is given, sticky cells also show their wild right away (used before the reveal).
     */
    public void showStickyWilds(boolean[][] sticky, Symbol[][] grid) {
        for (int reel = 0; reel < reels.length; reel++) {
            for (int row = 0; row < rowCount; row++) {
                Cell cell = reels[reel].getCell(row);
                cell.setSticky(sticky[reel][row]);
                if (grid != null && sticky[reel][row]) {
                    cell.setSymbol(grid[reel][row]);
                }
            }
        }
    }

    public void clearStickyWilds() {
        for (Reel reel : reels) {
            for (int row = 0; row < rowCount; row++) {
                reel.getCell(row).setSticky(false);
            }
        }
    }

    public void clearHighlights() {
        for (Reel reel : reels) {
            for (int row = 0; row < rowCount; row++) {
                reel.getCell(row).setLook(Cell.Look.NORMAL);
            }
        }
    }

    // ---------------------------------------------------------------- avalanche visuals

    /** Winning cells go empty, the rest stay where they are. */
    private void clearCells(boolean[][] removed) {
        for (int reel = 0; reel < reels.length; reel++) {
            for (int row = 0; row < rowCount; row++) {
                if (removed[reel][row]) {
                    reels[reel].getCell(row).setEmpty();
                    reels[reel].getCell(row).setSticky(false);   // a sticky wild that won is used up
                }
            }
        }
    }

    /**
     * The final grid, but with the new symbols not arrived yet: the new cells stay empty,
     * and the survivors already sit at the bottom.
     */
    private void showFallen(Symbol[][] gridAfter, boolean[][] newCells) {
        for (int reel = 0; reel < reels.length; reel++) {
            Symbol[] column = gridAfter[reel].clone();
            for (int row = 0; row < rowCount; row++) {
                if (newCells[reel][row]) {
                    column[row] = null;      // null = empty cell
                }
            }
            reels[reel].showSymbols(column);
        }
    }

    private void showGrid(Symbol[][] grid) {
        for (int reel = 0; reel < reels.length; reel++) {
            reels[reel].showSymbols(grid[reel]);
        }
    }

    // ---------------------------------------------------------------- win visuals

    private void highlight(Win win) {
        for (int reel = 0; reel < reels.length; reel++) {
            for (int row = 0; row < rowCount; row++) {
                Cell cell = reels[reel].getCell(row);
                cell.setLook(win.includes(reel, row) ? Cell.Look.WIN : Cell.Look.DIMMED);
            }
        }
    }

    /** The whole row in magenta (row BONUS), everything else dimmed. */
    private void highlightRow(int bonusRow) {
        for (int reel = 0; reel < reels.length; reel++) {
            for (int row = 0; row < rowCount; row++) {
                reels[reel].getCell(row).setLook(row == bonusRow ? Cell.Look.BONUS : Cell.Look.DIMMED);
            }
        }
    }

    private void showBanner(String text) {
        winLabel.setText(text);
        winBanner.pack();   // resize the banner to fit the new text
        winBanner.setPosition(getWidth() / 2f, 0f, Align.center);
        winBanner.setVisible(true);
    }

    /** "Ruby x4   6 ways   1.80"  or in free spins  "Ruby x4   6 ways   1.80 x3 = 5.40" */
    private static String bannerText(Win win, SpinResult.Cascade cascade, long betCents) {
        String ways = win.ways == 1 ? "1 way" : win.ways + " ways";
        String text = win.symbol.displayName() + " x" + win.reelCount + "   " + ways
                + "   " + Wallet.format(win.amount(betCents));
        if (cascade.multiplier > 1) {
            text += " x" + cascade.multiplier + " = " + Wallet.format(cascade.amount(win, betCents));
        }
        return text;
    }

    /** "5 UNIQUE GEMS!   BONUS 5.00"  or with a multiplier  "5 UNIQUE GEMS!   BONUS 5.00 x7 = 35.00" */
    private static String rowBonusText(SpinResult spin, int row, long betCents) {
        long basePay = Math.round(spin.rowBonusBasePay(row) * betCents);
        String text = "5 UNIQUE GEMS!   BONUS " + Wallet.format(basePay);
        if (spin.multiplierAfter > 1) {
            text += " x" + spin.multiplierAfter + " = " + Wallet.format(spin.rowBonusAmount(row, betCents));
        }
        return text;
    }

    /** "Topaz SHATTERED!   0.50"  or with a multiplier  "Topaz SHATTERED!   0.50 x7 = 3.50" */
    private static String shatterText(SpinResult spin, int index, long betCents) {
        int multiplier = spin.multiplierAfter;
        long total = spin.shatterAmount(index, betCents);
        String text = spin.shatterGem(index).displayName() + " SHATTERED!   ";
        if (multiplier > 1) {
            long base = Math.round(spin.shatterPay(index) / multiplier * betCents);
            text += Wallet.format(base) + " x" + multiplier + " = " + Wallet.format(total);
        } else {
            text += Wallet.format(total);
        }
        return text;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(background.r, background.g, background.b, background.a * parentAlpha);
        batch.draw(Assets.white, getX(), getY(), getWidth(), getHeight());
        super.draw(batch, parentAlpha);   // draws the reels, then the banner and message
    }
}