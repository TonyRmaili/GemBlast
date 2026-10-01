package gemblast;

/**
 * One winning combination, e.g. "Ruby on 4 reels, 6 ways, pays 0.75 per way".
 * Pure data, no graphics.
 */
public final class Win {

    public final Symbol symbol;
    public final int reelCount;      // how many reels in a row matched (3, 4 or 5)
    public final int ways;           // product of matching cells per reel, e.g. 2*1*3 = 6
    public final double payPerWay;   // from the paytable, as a multiple of the bet
    private final boolean[][] cells; // cells[reel][row] = true if that cell is part of this win

    public Win(Symbol symbol, int reelCount, int ways, double payPerWay, boolean[][] cells) {
        this.symbol = symbol;
        this.reelCount = reelCount;
        this.ways = ways;
        this.payPerWay = payPerWay;
        this.cells = cells;
    }

    /** Total win as a multiple of the bet: pay per way x number of ways. */
    public double multiplier() {
        return payPerWay * ways;
    }

    /** Win in cents for a given bet in cents. Math.round removes tiny double errors (30.000000004 -> 30). */
    public long amount(long betCents) {
        return Math.round(multiplier() * betCents);
    }

    public boolean includes(int reel, int row) {
        return cells[reel][row];
    }

    @Override
    public String toString() {
        return symbol + " x" + reelCount + ", " + ways + " ways, " + multiplier() + "x bet";
    }
}