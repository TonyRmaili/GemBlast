package gemblast;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import java.util.function.Consumer;

/**
 * Builds the shop's content (shown inside an InfoPanel): one row per feature buy / booster.
 * Built fresh every time the shop opens, so buttons always reflect the current state.
 */
public final class ShopContent {

    private static final Color HEADING = new Color(1.00f, 0.85f, 0.30f, 1f);
    private static final Color DIM     = new Color(0.70f, 0.70f, 0.78f, 1f);
    private static final Color ACTIVE  = new Color(0.40f, 1.00f, 0.50f, 1f);
    private static final Color ROW_BG  = new Color(0.20f, 0.20f, 0.26f, 1f);

    private ShopContent() { }

    /**
     * @param activeBooster  the booster currently on (GameMode.NORMAL = none)
     * @param locked         true while a round is running: everything is shown but nothing can be clicked
     * @param onChoose       called with the chosen mode (Main decides: buy it, or switch the booster on/off)
     */
    public static Table build(GameMode activeBooster, Wallet wallet, long betCents, boolean locked,
                              Consumer<GameMode> onChoose) {
        Table content = new Table();
        content.top();
        content.defaults().growX();

        content.add(label("FEATURE BUYS", 1.6f, HEADING)).left().padBottom(8f).row();
        content.add(label("Pay once, the bonus starts immediately.", 1.0f, DIM)).left().padBottom(8f).row();
        for (GameMode mode : GameMode.values()) {
            if (mode.isBuy()) {
                // Rule: only one feature at a time, so buys are locked while a booster is on.
                boolean allowed = !locked && activeBooster == GameMode.NORMAL && wallet.canAfford(mode.cost(betCents));
                String reason = activeBooster != GameMode.NORMAL ? "turn the booster off first" : null;
                content.add(row(mode, betCents, "BUY", allowed, false, reason, onChoose)).padBottom(10f).row();
            }
        }

        content.add(label("BOOSTER BETS", 1.6f, HEADING)).left().padTop(20f).padBottom(8f).row();
        content.add(label("Stays on for every spin until you turn it off. The price replaces the normal bet.",
                1.0f, DIM)).left().padBottom(8f).row();
        for (GameMode mode : GameMode.values()) {
            if (mode.isBooster()) {
                boolean isOn = activeBooster == mode;
                boolean otherOn = activeBooster != GameMode.NORMAL && !isOn;
                boolean allowed = !locked && !otherOn;
                String reason = otherOn ? "only one booster at a time" : null;
                content.add(row(mode, betCents, isOn ? "TURN OFF" : "TURN ON", allowed, isOn, reason, onChoose))
                        .padBottom(10f).row();
            }
        }
        return content;
    }

    /** One shop row: name + description on the left, price and button on the right. */
    private static Table row(GameMode mode, long betCents, String buttonText, boolean allowed, boolean isOn,
                             String reason, Consumer<GameMode> onChoose) {
        Table row = new Table();
        row.setBackground(Assets.solid(ROW_BG));
        row.pad(12f, 16f, 12f, 16f);

        Table text = new Table();
        text.add(label(mode.displayName + (isOn ? "   (ACTIVE)" : ""), 1.4f, isOn ? ACTIVE : Color.WHITE)).left().row();
        text.add(label(mode.description, 1.0f, DIM)).left().row();
        if (reason != null) {
            text.add(label(reason, 1.0f, DIM)).left().row();
        }

        String priceText = Wallet.format(mode.cost(betCents)) + (mode.isBooster() ? " / spin" : "");
        Label price = label(priceText, 1.4f, HEADING);
        price.setAlignment(Align.right);

        Button button = new Button(buttonText, 170f, 50f, () -> onChoose.accept(mode));
        button.setEnabled(allowed);

        row.add(text).expandX().left();
        row.add(price).width(180f).right().padRight(16f);
        row.add(button).size(170f, 50f);
        return row;
    }

    private static Label label(String text, float scale, Color color) {
        Label label = new Label(text, new Label.LabelStyle(Assets.font, color));
        label.setFontScale(scale);
        return label;
    }
}