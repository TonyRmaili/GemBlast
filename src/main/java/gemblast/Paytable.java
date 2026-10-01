package gemblast;

import java.util.EnumMap;

/**
 * How much each symbol pays for 3, 4 or 5 of a kind, as a multiplier of the TOTAL bet, per way.
 * Pure data, no graphics: the info screen, the win evaluator and the simulator all read from here,
 * so there is only ONE place where pay values live.
 */
public final class Paytable {

    public static final int MIN_COUNT = 3;   // fewest reels in a row that pay

    private final EnumMap<Symbol, double[]> pays = new EnumMap<>(Symbol.class);

    /**
     * "double... values" is a varargs parameter: you can pass any number of doubles,
     * and inside the method they arrive as an array. set(RUBY, 0.3, 0.75, 2) -> values = {0.3, 0.75, 2}.
     * It returns "this" so calls can be chained: new Paytable().set(...).set(...)
     */
    public Paytable set(Symbol symbol, double... values) {
        pays.put(symbol, values.clone());
        return this;
    }

    /** Pay for `count` reels in a row, or 0 if the symbol doesn't pay (wild, scatter, too few reels). */
    public double pay(Symbol symbol, int count) {
        double[] values = pays.get(symbol);
        if (values == null || count < MIN_COUNT) {
            return 0.0;
        }
        int index = Math.min(count - MIN_COUNT, values.length - 1);  // index 0 = 3 of a kind
        return values[index];
    }

    public boolean isPaying(Symbol symbol) {
        return pays.containsKey(symbol);
    }

    /** Highest count that has its own value (5 with three values: 3, 4, 5). */
    public int maxCount(Symbol symbol) {
        double[] values = pays.get(symbol);
        return values == null ? 0 : MIN_COUNT + values.length - 1;
    }

    /** Placeholder values, to be tuned for RTP later. */
    public static Paytable baseGame() {
        //                                3     4     5  of a kind
        return new Paytable()
                .set(Symbol.BLACK_DIAMOND, 0.50, 1.50, 5.00)
                .set(Symbol.DIAMOND,       0.40, 1.00, 3.00)
                .set(Symbol.RUBY,          0.30, 0.75, 2.00)
                .set(Symbol.OPAL,          0.25, 0.50, 1.50)
                .set(Symbol.SAPPHIRE,      0.10, 0.25, 0.75)
                .set(Symbol.EMERALD,       0.10, 0.20, 0.60)
                .set(Symbol.TOPAZ,         0.05, 0.15, 0.50)
                .set(Symbol.QUARTZ,        0.05, 0.10, 0.40);
    }
}