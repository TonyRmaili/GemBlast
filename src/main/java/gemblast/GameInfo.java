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
 * Builds the content of the info panel: paytable, rules, and the developer maths section.
 * Everything is generated from Paytable / ReelWeights, so the screen can never disagree with the game.
 */
public final class GameInfo {

    private static final int SYMBOLS_PER_ROW = 4;
    private static final float SYMBOL_SIZE = 110f;

    private static final Color HEADING   = new Color(1.00f, 0.85f, 0.30f, 1f);
    private static final Color TEXT      = Color.WHITE;
    private static final Color DIM       = new Color(0.70f, 0.70f, 0.78f, 1f);
    private static final Color FRAME     = new Color(0.35f, 0.45f, 0.75f, 1f);
    private static final Color TABLE_ROW = new Color(0.20f, 0.20f, 0.26f, 1f);

    private GameInfo() { }

    public static Table build(ReelWeights weights, Paytable paytable, int reels, int rows) {
        Table content = new Table();
        content.top();
        content.defaults().left();   // every cell added below is left-aligned unless it says otherwise

        heading(content, "PAYTABLE");
        content.add(paytableGrid(paytable)).center().row();
        note(content, "Values are multipliers of the total bet, per way.");

        heading(content, "RULES");
        content.add(wrapped(rulesText(reels, rows))).growX().row();

        heading(content, "MATH (DEVELOPER)");
        content.add(wrapped(mathSummary(weights, paytable, rows))).growX().row();
        content.add(probabilityTable(weights, paytable, rows)).padTop(12f).row();
        note(content, "Single drop only: avalanche and free spins not included yet.");

        return content;
    }

    // ---------------------------------------------------------------- paytable

    /** Grid of symbol cards, 4 per row, in the order the Symbol enum lists them. */
    private static Table paytableGrid(Paytable paytable) {
        Table grid = new Table();
        int column = 0;
        for (Symbol symbol : Symbol.values()) {
            if (!paytable.isPaying(symbol)) {
                continue;                       // skip wild and scatter
            }
            grid.add(symbolCard(symbol, paytable)).pad(12f, 18f, 12f, 18f);
            column++;
            if (column % SYMBOLS_PER_ROW == 0) {
                grid.row();
            }
        }
        return grid;
    }

    /** One card: framed symbol image with "5 | 5.00" rows below it (highest count first). */
    private static Table symbolCard(Symbol symbol, Paytable paytable) {
        Table frame = new Table();
        frame.setBackground(solid(FRAME));
        frame.add(new Image(Assets.symbol(symbol))).size(SYMBOL_SIZE).pad(3f);  // 3px of frame shows around it

        Table pays = new Table();
        for (int count = paytable.maxCount(symbol); count >= Paytable.MIN_COUNT; count--) {
            pays.add(label(String.valueOf(count), 1.2f, DIM)).width(24f).right();
            pays.add(new Image(solid(DIM))).width(2f).height(24f).padLeft(10f).padRight(10f);
            pays.add(label(money(paytable.pay(symbol, count)), 1.2f, TEXT)).width(60f).left();
            pays.row().padTop(4f);
        }

        Table card = new Table();
        card.add(frame).row();
        card.add(pays).padTop(10f);
        return card;
    }

    // ---------------------------------------------------------------- rules

    private static String rulesText(int reels, int rows) {
        int ways = (int) Math.pow(rows, reels);
        // Java 8 has no multi-line text blocks, so String.join puts a new line between the pieces.
        return String.join("\n",
                reels + " reels, " + rows + " rows, " + ways + " ways to win.",
                "Matching symbols on adjacent reels from the leftmost reel pay, on any row.",
                "Wins per way are multiplied: 2 rubies on reel 1 and 2 on reel 2 = 4 ways.",
                "Star Wild substitutes for all paying symbols. It does not land on reel 1.",
                "Winning symbols are removed, the symbols above fall down and new ones fill the gaps.",
                "This repeats as long as there are new wins (avalanche).",
                "",
                "FREE SPINS",
                "Scatters are counted when the spin is over, after all avalanches.",
                GameRules.FREE_SPINS_SCATTERS + " scatters award " + GameRules.FREE_SPINS_AWARDED + " free spins.",
                GameRules.SUPER_SCATTERS + " or more scatters award " + GameRules.FREE_SPINS_AWARDED + " SUPER free spins.",
                "A persistent multiplier starts at x" + GameRules.MULTIPLIER_START
                        + " and grows by " + GameRules.MULTIPLIER_STEP + " after every winning avalanche step.",
                "It never resets during the bonus: every win is multiplied by its current value.",
                GameRules.RETRIGGER_SCATTERS + " or more scatters during free spins add "
                        + GameRules.RETRIGGER_SPINS + " spins.",
                "",
                "",
                "SUPER FREE SPINS",
                "Everything from free spins, plus STICKY WILDS: every wild that appears locks in place",
                "(it doesn't fall and stays for the next spins) until it is part of a win; then it is used up.",
                "",
                "Feature buys and booster bets: coming soon.");
    }

    // ---------------------------------------------------------------- developer maths

    private static String mathSummary(ReelWeights weights, Paytable paytable, int rows) {
        double rtp = GameMath.waysRtp(weights, paytable, rows);
        double trigger = GameMath.scatterChance(weights, rows, 3);
        return String.join("\n",
                "Ways RTP (exact): " + percent(rtp),
                "3+ scatters on the first grid (exact, before avalanches): " + percent(trigger)
                        + "  (1 in " + String.format(Locale.US, "%.0f", 1.0 / trigger) + " spins)");
    }

    /** Rows = symbols, columns = land chance on each reel + that symbol's share of RTP. */
    private static Table probabilityTable(ReelWeights weights, Paytable paytable, int rows) {
        Table table = new Table();
        table.defaults().pad(4f, 8f, 4f, 8f).right();

        table.add(label("Symbol", 1.1f, HEADING)).left();
        for (int reel = 0; reel < weights.reelCount(); reel++) {
            table.add(label("Reel " + (reel + 1), 1.1f, HEADING));
        }
        table.add(label("RTP", 1.1f, HEADING));
        table.row();

        boolean shaded = false;
        for (Symbol symbol : Symbol.values()) {
            Drawable rowBackground = shaded ? solid(TABLE_ROW) : null;   // alternate row shading
            shaded = !shaded;

            table.add(cell(prettyName(symbol), rowBackground, true));
            for (int reel = 0; reel < weights.reelCount(); reel++) {
                table.add(cell(percent(weights.probability(reel, symbol)), rowBackground, false)).fill();
            }
            String rtp = paytable.isPaying(symbol)
                    ? percent(GameMath.symbolRtp(weights, paytable, symbol, rows))
                    : "-";
            table.add(cell(rtp, rowBackground, false)).fill();
            table.row();
        }
        return table;
    }

    private static Table cell(String text, Drawable background, boolean alignLeft) {
        Table cell = new Table();
        cell.setBackground(background);
        Label label = label(text, 1.0f, TEXT);
        if (alignLeft) {
            cell.add(label).expandX().left().pad(0f, 6f, 0f, 6f);
        } else {
            cell.add(label).expandX().right().pad(0f, 6f, 0f, 6f);
        }
        return cell;
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

    private static String percent(double fraction) {
        return String.format(Locale.US, "%.2f%%", fraction * 100.0);   // "%%" prints a literal %
    }

    /** BLACK_DIAMOND -> "Black Diamond" */
    private static String prettyName(Symbol symbol) {
        StringBuilder sb = new StringBuilder();
        for (String word : symbol.name().split("_")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(word.charAt(0)).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private static Drawable solid(Color color) {
        return new TextureRegionDrawable(new TextureRegion(Assets.white)).tint(color);
    }
}