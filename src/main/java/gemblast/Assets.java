package gemblast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.EnumMap;

/** Loads and owns every texture/font. Call load() once at startup and dispose() at shutdown. */
public final class Assets {

    public static Texture white;      // 1x1 white pixel, tinted to draw any coloured rectangle
    public static BitmapFont font;    // libGDX's built-in default font

    private static final EnumMap<Symbol, Texture> symbolTextures = new EnumMap<>(Symbol.class);

    private Assets() { }  // static-only class, nobody should create an instance

    public static void load() {
        white = createSolidTexture(Color.WHITE);

        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.getData().setScale(2f);

        for (Symbol symbol : Symbol.values()) {
            FileHandle file = Gdx.files.internal(symbol.texturePath);
            if (file.exists()) {
                Texture texture = new Texture(file);
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                symbolTextures.put(symbol, texture);
            } else {
                // Keeps the game running even if a file name is wrong; check the console.
                Gdx.app.error("Assets", "Missing " + symbol.texturePath + " -> using a coloured placeholder");
                symbolTextures.put(symbol, createSolidTexture(symbol.placeholderColor));
            }
        }
    }

    public static Texture symbol(Symbol symbol) {
        return symbolTextures.get(symbol);
    }

    private static Texture createSolidTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public static void dispose() {
        white.dispose();
        font.dispose();
        for (Texture texture : symbolTextures.values()) {
            texture.dispose();
        }
        symbolTextures.clear();
    }

    public static Drawable solid(Color color) {
        return new TextureRegionDrawable(new TextureRegion(white)).tint(color);
    }
}