package gemblast;

import com.badlogic.gdx.graphics.Color;

/**
 * Every symbol that can appear on the grid.
 *
 * An enum is a fixed list of named constants. Java gives each one an index
 * (ordinal()) in the order they are written below: BLACK_DIAMOND = 0,
 * DIAMOND = 1 ... SCATTER = 9. ReelWeights relies on that order, so if you
 * reorder or add symbols here, update the weight tables too.
 */
public enum Symbol {
    //            texture file (inside assets/)          placeholder colour if file is missing
    BLACK_DIAMOND("assets/slotSymbols/blackDiamond.jpg",  new Color(0.25f, 0.25f, 0.30f, 1f)),
    DIAMOND      ("assets/slotSymbols/diamond.jpg",       new Color(0.85f, 0.95f, 1.00f, 1f)),
    RUBY         ("assets/slotSymbols/ruby.jpg",          new Color(0.85f, 0.10f, 0.20f, 1f)),
    OPAL         ("assets/slotSymbols/opal.jpg",          new Color(0.95f, 0.75f, 0.85f, 1f)),
    SAPPHIRE     ("assets/slotSymbols/sapphire.jpg",      new Color(0.15f, 0.30f, 0.90f, 1f)),
    EMERALD      ("assets/slotSymbols/emerald.jpg",       new Color(0.10f, 0.75f, 0.35f, 1f)),
    TOPAZ        ("assets/slotSymbols/topaz.jpg",         new Color(1.00f, 0.60f, 0.10f, 1f)),
    QUARTZ       ("assets/slotSymbols/quartz.jpg",        new Color(0.80f, 0.80f, 0.85f, 1f)),
    WILD         ("assets/slotSymbols/starWild.jpg",      new Color(1.00f, 0.85f, 0.00f, 1f)),
    SCATTER      ("assets/slotSymbols/luckyScatter.jpg",  new Color(0.70f, 0.20f, 0.90f, 1f));

    public final String texturePath;
    public final Color placeholderColor;

    Symbol(String texturePath, Color placeholderColor) {
        this.texturePath = texturePath;
        this.placeholderColor = placeholderColor;
    }

    /** BLACK_DIAMOND -> "Black Diamond" */
    public String displayName() {
        StringBuilder sb = new StringBuilder();
        for (String word : name().split("_")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(word.charAt(0)).append(word.substring(1).toLowerCase(java.util.Locale.ROOT));
        }
        return sb.toString();
    }
}