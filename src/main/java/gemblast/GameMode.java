package gemblast;

/**
 * The ways a round can be played. NORMAL is a regular spin; the others come from the shop.
 * What each mode costs is maths, so it lives in SlotMath.price(mode).
 */
public enum GameMode {

    //                    name                  type          description
    NORMAL              ("Normal spin",         Type.NORMAL,  "A regular spin."),
    BUY_FREE_SPINS      ("Free Spins",          Type.BUY,     "Start free spins right away."),
    BUY_SUPER_FREE_SPINS("Super Free Spins",    Type.BUY,     "Start super free spins (with sticky wilds) right away."),
    BOOST_SCATTER       ("Scatter Booster",     Type.BOOSTER, "Every spin: a guaranteed scatter on the last reel."),
    BOOST_WILD          ("Wild Booster",        Type.BOOSTER, "Every spin: a guaranteed wild on a random cell of reels 2-5.");

    /** An enum can contain its own small enum. */
    public enum Type { NORMAL, BUY, BOOSTER }

    public final String displayName;
    public final Type type;
    public final String description;

    GameMode(String displayName, Type type, String description) {
        this.displayName = displayName;
        this.type = type;
        this.description = description;
    }

    public boolean isBuy() {
        return type == Type.BUY;
    }

    public boolean isBooster() {
        return type == Type.BOOSTER;
    }
}
