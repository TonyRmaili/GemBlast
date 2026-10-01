package gemblast;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Shows the balance: a small "BALANCE" caption above the amount.
 * It only DISPLAYS; the Wallet owns the actual number.
 */
public class BalanceDisplay extends Table {

    private static final Color BACKGROUND = new Color(0.12f, 0.12f, 0.16f, 1f);
    private static final Color CAPTION    = new Color(0.70f, 0.70f, 0.78f, 1f);
    private static final Color TOO_LOW    = new Color(1.00f, 0.35f, 0.35f, 1f);

    private final Label valueLabel;

    public BalanceDisplay(float width, float height) {
        setSize(width, height);
        setBackground(solid(BACKGROUND));

        Label caption = new Label("BALANCE", new Label.LabelStyle(Assets.font, CAPTION));
        caption.setFontScale(1.0f);

        valueLabel = new Label("", new Label.LabelStyle(Assets.font, Color.WHITE));
        valueLabel.setFontScale(1.6f);

        add(caption).row();
        add(valueLabel).padTop(2f);
    }

    /** Red text when the player can't afford another spin. */
    public void show(long balanceCents, boolean canAffordBet) {
        valueLabel.setText(Wallet.format(balanceCents));
        valueLabel.setColor(canAffordBet ? Color.WHITE : TOO_LOW);
    }

    private static Drawable solid(Color color) {
        return new TextureRegionDrawable(new TextureRegion(Assets.white)).tint(color);
    }
}