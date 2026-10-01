package gemblast;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * A small panel: caption on top, value below. Used for BALANCE, WIN, FREE SPINS and MULTIPLIER.
 * (Replaces BalanceDisplay: same thing, but the caption is a parameter, so one class serves both.)
 */
public class AmountDisplay extends Table {

    private static final Color BACKGROUND = new Color(0.12f, 0.12f, 0.16f, 1f);
    private static final Color CAPTION    = new Color(0.70f, 0.70f, 0.78f, 1f);
    private static final Color WARNING    = new Color(1.00f, 0.35f, 0.35f, 1f);

    private final Label valueLabel;

    public AmountDisplay(String caption, float width, float height) {
        setSize(width, height);
        setBackground(Assets.solid(BACKGROUND));

        Label captionLabel = new Label(caption, new Label.LabelStyle(Assets.font, CAPTION));
        captionLabel.setFontScale(1.0f);

        valueLabel = new Label("0.00", new Label.LabelStyle(Assets.font, Color.WHITE));
        valueLabel.setFontScale(1.6f);

        add(captionLabel).row();
        add(valueLabel).padTop(2f);
    }

    public void setAmount(long cents) {
        valueLabel.setText(Wallet.format(cents));
    }

    /** Any text instead of an amount, e.g. "3 / 10" or "x4". */
    public void setText(String text) {
        valueLabel.setText(text);
    }

    /** Red text, e.g. when the balance is too low for another spin. */
    public void setWarning(boolean warning) {
        valueLabel.setColor(warning ? WARNING : Color.WHITE);
    }
}