package gemblast;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;

/** One square on the grid. Shows either nothing (dark box) or a symbol, in one of three looks. */
public class Cell extends Actor {

    /** NORMAL = plain, WIN = gold frame (part of the win being shown), DIMMED = faded (not part of it). */
    public enum Look { NORMAL, WIN, DIMMED }

    private static final Color EMPTY_COLOR = new Color(0.05f, 0.05f, 0.08f, 1f);
    private static final Color WIN_FRAME   = new Color(1.00f, 0.80f, 0.20f, 1f);
    private static final Color STICKY_FRAME = new Color(0.25f, 0.85f, 1.00f, 1f);  // cyan: sticky wild
    private static final float SYMBOL_INSET = 0.08f;   // 8% margin so symbols don't touch the edges
    private static final float FRAME_WIDTH  = 5f;
    private static final float DIMMED_ALPHA = 0.3f;

    private Symbol symbol;            // null means empty
    private Look look = Look.NORMAL;
    private boolean sticky = false;    // super free spins: a locked wild sits here

    public Cell(float size) {
        setSize(size, size);
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public void setEmpty() {
        this.symbol = null;
        this.look = Look.NORMAL;
    }

    /** Not cleared by setEmpty(): the ReelScreen sets it from the engine's sticky mask. */
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    public void setLook(Look look) {
        this.look = look;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float x = getX(), y = getY(), w = getWidth(), h = getHeight();

        // 1) frame: draw a coloured square, then the background slightly smaller on top of it.
        //    Gold (part of the win being shown) wins over cyan (sticky wild).
        Color frame = (look == Look.WIN) ? WIN_FRAME : (sticky ? STICKY_FRAME : null);
        if (frame != null) {
            batch.setColor(frame.r, frame.g, frame.b, parentAlpha);
            batch.draw(Assets.white, x, y, w, h);
            x += FRAME_WIDTH;  y += FRAME_WIDTH;  w -= FRAME_WIDTH * 2f;  h -= FRAME_WIDTH * 2f;
        }

        // 2) background box
        batch.setColor(EMPTY_COLOR.r, EMPTY_COLOR.g, EMPTY_COLOR.b, EMPTY_COLOR.a * parentAlpha);
        batch.draw(Assets.white, x, y, w, h);

        // 3) symbol on top (if any), faded when dimmed
        if (symbol != null) {
            float alpha = (look == Look.DIMMED) ? DIMMED_ALPHA : 1f;
            float inset = getWidth() * SYMBOL_INSET;
            batch.setColor(1f, 1f, 1f, alpha * parentAlpha);
            batch.draw(Assets.symbol(symbol),
                    getX() + inset, getY() + inset,
                    getWidth() - inset * 2f, getHeight() - inset * 2f);
        }
    }
}