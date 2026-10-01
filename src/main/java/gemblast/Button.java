package gemblast;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * A minimal clickable button: coloured box + centred text.
 * (libGDX's TextButton needs a Skin file; this avoids that for now.)
 */
public class Button extends Actor {

    private static final Color NORMAL   = new Color(0.20f, 0.55f, 0.30f, 1f);
    private static final Color PRESSED  = new Color(0.15f, 0.40f, 0.22f, 1f);
    private static final Color DISABLED = new Color(0.30f, 0.30f, 0.32f, 1f);

    private final String text;
    private final GlyphLayout layout = new GlyphLayout();  // measures text so we can centre it
    private final ClickListener clickListener;
    private boolean enabled = true;

    public Button(String text, float width, float height, Runnable onClick) {
        this.text = text;
        setSize(width, height);

        clickListener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (enabled) {
                    onClick.run();
                }
            }
        };
        addListener(clickListener);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color bg = !enabled ? DISABLED : (clickListener.isPressed() ? PRESSED : NORMAL);
        batch.setColor(bg.r, bg.g, bg.b, bg.a * parentAlpha);
        batch.draw(Assets.white, getX(), getY(), getWidth(), getHeight());

        BitmapFont font = Assets.font;
        font.setColor(1f, 1f, 1f, parentAlpha);
        layout.setText(font, text);
        font.draw(batch, layout,
                getX() + (getWidth()  - layout.width)  / 2f,
                getY() + (getHeight() + layout.height) / 2f);  // font y = top of the text
    }
}