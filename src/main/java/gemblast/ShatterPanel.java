package gemblast;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.List;

/**
 * The SHATTER meters, shown next to the reels during free spins: one row per gem with its icon,
 * a bar of SlotMath.SHATTER_METER_SIZE segments and "7/10". Only displays numbers it is given;
 * the counting and the explosions are decided by the engine and SlotMath.
 */
public class ShatterPanel extends Table {

    private static final Color PANEL_BG     = new Color(0.10f, 0.06f, 0.16f, 0.92f);
    private static final Color SEGMENT_OFF  = new Color(0.22f, 0.20f, 0.30f, 1f);
    private static final Color SEGMENT_ON   = new Color(0.30f, 0.85f, 1.00f, 1f);   // filling up: icy blue
    private static final Color SEGMENT_FULL = new Color(1.00f, 0.80f, 0.20f, 1f);   // full, about to explode: gold
    private static final Color FLASH_COLOR  = new Color(1.00f, 0.55f, 0.15f, 1f);   // the explosion flash
    private static final Color TEXT         = new Color(0.85f, 0.85f, 0.92f, 1f);

    private static final float ICON_SIZE = 40f;
    private static final float BAR_WIDTH = 100f;
    private static final float BAR_HEIGHT = 14f;

    /** One gem's row: everything that changes when its meter changes. */
    private static final class GemRow {
        final Symbol gem;
        final Image icon;
        final Image[] segments;
        final Label countLabel;
        final Image flash;

        GemRow(Symbol gem, Image icon, Image[] segments, Label countLabel, Image flash) {
            this.gem = gem;
            this.icon = icon;
            this.segments = segments;
            this.countLabel = countLabel;
            this.flash = flash;
        }
    }

    private final List<GemRow> rows = new ArrayList<>();

    /** @param config only used to know which symbols are gems (the ones with pays). */
    public ShatterPanel(ValueConfig config) {
        setBackground(Assets.solid(PANEL_BG));
        pad(10f);
        defaults().padTop(4f).padBottom(4f);

        Label title = new Label("SHATTER", new Label.LabelStyle(Assets.font, new Color(1f, 0.85f, 0.3f, 1f)));
        title.setFontScale(1.0f);
        add(title).padBottom(8f).row();

        int size = SlotMath.SHATTER_METER_SIZE;
        float segmentWidth = BAR_WIDTH / size;
        for (Symbol gem : Symbol.values()) {
            if (config.pays(gem) == null) {
                continue;                            // wild and scatter have no meter
            }
            Image icon = new Image(Assets.symbol(gem));

            Table bar = new Table();
            Image[] segments = new Image[size];
            for (int i = 0; i < size; i++) {
                segments[i] = new Image(Assets.solid(Color.WHITE));
                segments[i].setColor(SEGMENT_OFF);
                bar.add(segments[i]).size(segmentWidth - 1f, BAR_HEIGHT).padRight(1f);
            }

            Label countLabel = new Label("0/" + size, new Label.LabelStyle(Assets.font, TEXT));
            countLabel.setFontScale(0.8f);
            countLabel.setAlignment(Align.right);

            Table line = new Table();
            line.add(icon).size(ICON_SIZE).padRight(6f);
            line.add(bar).padRight(6f);
            line.add(countLabel).width(46f);

            // The flash sits behind the row and is invisible until the gem explodes.
            Image flash = new Image(Assets.solid(Color.WHITE));
            flash.setColor(FLASH_COLOR.r, FLASH_COLOR.g, FLASH_COLOR.b, 0f);

            Stack stack = new Stack();
            stack.add(flash);
            stack.add(line);
            add(stack).fillX().row();

            rows.add(new GemRow(gem, icon, segments, countLabel, flash));
        }
        pack();
    }

    /** Shows these meter values (indexed by Symbol.ordinal()). Null or missing = all empty. */
    public void setMeters(int[] meters) {
        int size = SlotMath.SHATTER_METER_SIZE;
        for (GemRow row : rows) {
            int value = meters == null ? 0 : meters[row.gem.ordinal()];
            int shown = Math.min(value, size);           // above the size: shown as full until it explodes
            boolean full = value >= size;
            for (int i = 0; i < size; i++) {
                row.segments[i].setColor(i < shown ? (full ? SEGMENT_FULL : SEGMENT_ON) : SEGMENT_OFF);
            }
            row.countLabel.setText(Math.min(value, size) + "/" + size);
        }
    }

    public void reset() {
        setMeters(null);
        for (GemRow row : rows) {
            row.flash.clearActions();
            row.flash.getColor().a = 0f;
            row.icon.clearActions();
            row.icon.setScale(1f);
        }
    }

    /** The explosion: the row flashes orange and the gem icon pops. */
    public void explode(Symbol gem) {
        for (GemRow row : rows) {
            if (row.gem != gem) {
                continue;
            }
            row.flash.clearActions();
            row.flash.addAction(Actions.sequence(Actions.alpha(1f), Actions.fadeOut(0.9f)));
            row.icon.setOrigin(Align.center);                // scale around the middle, not the corner
            row.icon.clearActions();
            row.icon.addAction(Actions.sequence(
                    Actions.scaleTo(1.6f, 1.6f, 0.12f),
                    Actions.scaleTo(1f, 1f, 0.35f)));
        }
    }
}