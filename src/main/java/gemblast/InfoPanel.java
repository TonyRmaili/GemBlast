package gemblast;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * Full-screen overlay with a centred panel: title, close button, and scrollable text.
 *
 * Layout is done with Table: you add() actors cell by cell and call row() to start a new row,
 * instead of calculating x/y positions by hand.
 */
public class InfoPanel extends Table {

    private static final float DEFAULT_WIDTH = 1000f;
    private static final float HEIGHT = 600f;

    private final ScrollPane scrollPane;

    public InfoPanel(String title, Actor content, Runnable onClose) {
        this(title, content, onClose, DEFAULT_WIDTH);
    }

    /** @param width the panel's width; the CONFIG panel uses a wider one for its arrow buttons. */
    public InfoPanel(String title, Actor content, Runnable onClose, float width) {
        // --- the overlay itself (this Table) ---
        setFillParent(true);                    // cover the whole stage
        setBackground(solid(new Color(0f, 0f, 0f, 0.7f)));   // dark see-through dimming
        setTouchable(Touchable.enabled);        // swallow clicks so the SPIN button behind can't be pressed

        // --- styles built in code (no Skin file needed) ---
        Label.LabelStyle textStyle = new Label.LabelStyle(Assets.font, Color.WHITE);

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.vScroll = solid(new Color(0.10f, 0.10f, 0.14f, 1f));      // scrollbar track
        scrollStyle.vScrollKnob = solid(new Color(0.55f, 0.55f, 0.65f, 1f));  // the part you drag
        scrollStyle.vScrollKnob.setMinWidth(10f);
        scrollStyle.vScrollKnob.setMinHeight(40f);

        // --- title row ---
        Label titleLabel = new Label(title, textStyle);
        Button closeButton = new Button("X", 60f, 60f, onClose);

        // --- scrollable body ---
        Table padded = new Table();
        padded.add(content).growX().top().padRight(20f);   // room for the scrollbar
        padded.top();

        scrollPane = new ScrollPane(padded, scrollStyle);
        scrollPane.setScrollingDisabled(true, false);   // no sideways scrolling, only up/down
        scrollPane.setFadeScrollBars(false);            // keep the scrollbar visible

        // --- the panel in the middle ---
        Table panel = new Table();
        panel.setBackground(solid(new Color(0.15f, 0.15f, 0.20f, 1f)));
        panel.pad(24f);

        panel.add(titleLabel).left().expandX();         // row 1: title on the left...
        panel.add(closeButton).size(60f).right();       //        ...close button on the right
        panel.row();
        panel.add(scrollPane).colspan(2).grow().padTop(16f);  // row 2: text spans both columns

        add(panel).width(width).height(HEIGHT);         // a Table centres its content by default
    }

    /** The mouse wheel only scrolls the actor that has "scroll focus", so Main hands this to the Stage. */
    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    /** A plain coloured rectangle usable as a background, built from the 1x1 white pixel. */
    private static Drawable solid(Color color) {
        return new TextureRegionDrawable(new TextureRegion(Assets.white)).tint(color);
    }
}