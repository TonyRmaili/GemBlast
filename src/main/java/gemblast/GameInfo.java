package gemblast;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import java.util.Locale;

/**
 * Builds the content of the info panel: the paytable (straight from weights.json) and the rules.
 * Built once at startup, so it shows the paytable the game started with.
 */
public final class GameInfo {

    private static final int SYMBOLS_PER_ROW = 4;
    private static final float SYMBOL_SIZE = 110f;

    private static final Color HEADING = new Color(1.00f, 0.85f, 0.30f, 1f);
    private static final Color TEXT    = Color.WHITE;
    private static final Color DIM     = new Color(0.70f, 0.70f, 0.78f, 1f);
    private static final Color FRAME   = new Color(0.35f, 0.45f, 0.75f, 1f);

    private GameInfo() { }

    public static Table build(ValueConfig config, int reels, int rows) {
        Table content = new Table();
        content.top();
        content.defaults().left();   // every cell added below is left-aligned unless it says otherwise

        heading(content, "PAYTABLE");
        content.add(paytableGrid(config)).center().row();
        note(content, "Values are multipliers of the total bet, per way.");

        heading(content, "RULES");
        content.add(wrapped(rulesText(reels, rows))).growX().row();

        return content;
    }

    // ---------------------------------------------------------------- paytable

    /** Grid of symbol cards, 4 per row, in the order the Symbol enum lists them. */
    private static Table paytableGrid(ValueConfig config) {
        Table grid = new Table();
        int column = 0;
        for (Symbol symbol : Symbol.values()) {
            double[] pays = config.pays(symbol);
            if (pays == null) {
                continue;                       // not in the paytable: wild and scatter
            }
            grid.add(symbolCard(symbol, pays)).pad(12f, 18f, 12f, 18f);
            column++;
            if (column % SYMBOLS_PER_ROW == 0) {
                grid.row();
            }
        }
        return grid;
    }

    /** One card: framed symbol image with "5 | 5.00" rows below it (highest count first). */
    private static Table symbolCard(Symbol symbol, double[] pays) {
        Table frame = new Table();
        frame.setBackground(solid(FRAME));
        frame.add(new Image(Assets.symbol(symbol))).size(SYMBOL_SIZE).pad(3f);  // 3px of frame shows around it

        Table rowsTable = new Table();
        for (int i = pays.length - 1; i >= 0; i--) {
            int count = SlotMath.MIN_WIN_LENGTH + i;   // pays[0] = 3 of a kind, pays[1] = 4, ...
            rowsTable.add(label(String.valueOf(count), 1.2f, DIM)).width(24f).right();
            rowsTable.add(new Image(solid(DIM))).width(2f).height(24f).padLeft(10f).padRight(10f);
            rowsTable.add(label(money(pays[i]), 1.2f, TEXT)).width(60f).left();
            rowsTable.row().padTop(4f);
        }

        Table card = new Table();
        card.add(frame).row();
        card.add(rowsTable).padTop(10f);
        return card;
    }

    // ---------------------------------------------------------------- rules

    /** TODO: update this text when you settle your free spins rules (trigger counts, spins, multiplier). */
    private static String rulesText(int reels, int rows) {
        int ways = (int) Math.pow(rows, reels);
        // String.join puts a new line between the pieces.
        return String.join("\n",
                reels + " reels, " + rows + " rows, " + ways + " ways to win.",
                "Matching symbols on adjacent reels from the leftmost reel pay, on any row.",
                "Star Wild substitutes for all paying symbols.",
                "Winning symbols are removed, the symbols above fall down and new ones fill the gaps.",
                "This repeats as long as there are new wins (avalanche).",
                "",
                "FREE SPINS",
                "Lucky Scatters trigger free spins. Scatters are counted when the spin is over, after all avalanches.",
                "A persistent multiplier grows during the bonus and never resets until it ends.",
                "",
                "SUPER FREE SPINS",
                "Everything from free spins, plus STICKY WILDS: every wild that appears locks in place",
                "until it is part of a win; then it is used up.",
                "",
                "ROW BONUS (super free spins only)",
                "After the last avalanche of a spin, every row with 5 DIFFERENT gems (no wild, no scatter)",
                "pays the 5-of-a-kind pay of its best gem, times the current multiplier.",
                "",
                "SHATTER (both free spins modes)",
                "Every gem in a win shatters into its meter (wilds and scatters don't count).",
                "Meters carry over from spin to spin. At the end of a spin, every full meter ("
                        + SlotMath.SHATTER_METER_SIZE + ") explodes",
                "and pays that gem's 5-of-a-kind pay, times the current multiplier.");
    }

    // ---------------------------------------------------------------- small helpers

    private static void heading(Table content, String text) {
        content.add(label(text, 1.7f, HEADING)).padTop(26f).padBottom(10f).row();
    }

    private static void note(Table content, String text) {
        content.add(label(text, 1.0f, DIM)).padTop(6f).row();
    }

    private static Label label(String text, float scale, Color color) {
        Label label = new Label(text, new Label.LabelStyle(Assets.font, color));
        label.setFontScale(scale);
        return label;
    }

    private static Label wrapped(String text) {
        Label label = label(text, 1.2f, TEXT);
        label.setWrap(true);
        label.setAlignment(Align.topLeft);
        return label;
    }

    /** Locale.US forces a dot as decimal separator; on a Swedish PC "%.2f" would print "0,50". */
    private static String money(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static Drawable solid(Color color) {
        return new TextureRegionDrawable(new TextureRegion(Assets.white)).tint(color);
    }
}