package gemblast;

/**
 * One winning combination, e.g. "Ruby on 4 reels, 6 ways, 0.75 per way, 4.50x bet in total".
 * Only data: SlotMath.findWins works out every value and passes it in.
 */
public final class Win {

    public final Symbol symbol;
    public final int reelCount;      // how many reels in a row matched
    public final int ways;           // number of ways this win pays on
    public final double payPerWay;   // from the paytable, x bet
    public final double totalPay;    // the whole win, x bet (before any free spins multiplier)
    private final boolean[][] cells; // cells[reel][row] = true if that cell is part of this win

    public Win(Symbol symbol, int reelCount, int ways, double payPerWay, double totalPay, boolean[][] cells) {
        this.symbol = symbol;
        this.reelCount = reelCount;
        this.ways = ways;
        this.payPerWay = payPerWay;
        this.totalPay = totalPay;
        this.cells = cells;
    }

    /** The win as a multiple of the bet. */
    public double multiplier() {
        return totalPay;
    }

    /** The win in cents for a bet in cents. Math.round removes tiny double errors (30.000000004 -> 30). */
    public long amount(long betCents) {
        return Math.round(totalPay * betCents);
    }

    public boolean includes(int reel, int row) {
        return cells[reel][row];
    }

    @Override
    public String toString() {
        return symbol + " x" + reelCount + ", " + ways + " ways, " + totalPay + "x bet";
    }
}