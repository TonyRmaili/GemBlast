package gemblast;

/**
 * How a round is played and what it costs. NORMAL is a regular spin; the others come from the shop.
 *
 * Prices are "fair" prices: the mode's average return divided by the target RTP, so every mode
 * returns about the same percentage of what it costs. The Simulator measures the average returns;
 * update the costs here whenever the maths changes.
 *
 * Current prices: from the SIM panel / Simulator.main() "Fair price" line, with the UNTUNED maths
 * (normal spins currently return ~85%), priced at 96%. Re-measure after tuning.
 */
public enum GameMode {

    //                    name                    cost (x bet)  type          description
    NORMAL              ("Normal spin",                 1.00,  Type.NORMAL,  "A regular spin."),
    BUY_FREE_SPINS      ("Free Spins",                 24.35,  Type.BUY,     "Start " + GameRules.FREE_SPINS_AWARDED + " free spins right away."),
    BUY_SUPER_FREE_SPINS("Super Free Spins",           38.40,  Type.BUY,     "Start " + GameRules.FREE_SPINS_AWARDED + " super free spins (with sticky wilds) right away."),
    BOOST_SCATTER       ("Scatter Booster",             2.83,  Type.BOOSTER, "Every spin: a guaranteed scatter on the last reel."),
    BOOST_WILD          ("Wild Booster",                1.42,  Type.BOOSTER, "Every spin: a guaranteed wild on a random cell of reels 2-5.");

    /** An enum can contain its own small enum. */
    public enum Type { NORMAL, BUY, BOOSTER }

    public final String displayName;
    public final double costMultiplier;
    public final Type type;
    public final String description;

    GameMode(String displayName, double costMultiplier, Type type, String description) {
        this.displayName = displayName;
        this.costMultiplier = costMultiplier;
        this.type = type;
        this.description = description;
    }

    /** Price in cents for a given bet in cents. */
    public long cost(long betCents) {
        return Math.round(costMultiplier * betCents);
    }

    public boolean isBuy() {
        return type == Type.BUY;
    }

    public boolean isBooster() {
        return type == Type.BOOSTER;
    }
}