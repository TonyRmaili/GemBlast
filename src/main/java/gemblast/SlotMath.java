package gemblast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ALL the maths of Gem Blast lives in this one class: how symbols land, what pays, how the
 * avalanche refills, the free spins rules, the shop and the statistics formulas.
 *
 * Every method below is a TODO. The rest of the game (GameEngine, the screen, the simulator) only
 * CALLS these methods, so as soon as one is implemented the game uses it everywhere.
 * An unimplemented method throws, so a crash message tells you exactly which piece is missing.
 *
 * Numbers that are meant to be tuned (weights, paytable) are NOT in here: they come from
 * weights.json through ValueConfig, so you can change them without recompiling.
 */
public final class SlotMath {

    // ================================================================ rules (design choices, change freely)

    /** Wins need at least this many adjacent reels from the left. Also used by the paytable display. */
    public static final int MIN_WIN_LENGTH = 3;

    /** Return target used for fair shop prices and in the simulator report. */
    public static final double TARGET_RTP = 0.96;

    /** Safety limits so a loop can never run forever. Not part of the maths; leave them high. */
    public static final int MAX_CASCADES = 100;
    public static final int MAX_FREE_SPINS = 200;

    private final ValueConfig config;

    public SlotMath(ValueConfig config) {
        this.config = config;
    }

    public ValueConfig config() {
        return config;
    }

    // ================================================================ 1. how symbols land

    /**
     * One random symbol for one cell on the given reel (0 = leftmost), drawn with that reel's
     * weights from config.weights(reel). Weight w out of a reel total T means probability w / T.
     */
    public Symbol drawSymbol(int reel, Random rng) {

        double[] weights = config.weights(reel);
        double randomNumber = rng.nextDouble();
        double totalWeight =  ValueConfig.WEIGHT_TOTAL;
        randomNumber = randomNumber*totalWeight;

        double counter = 0;
        int weightIndex = weights.length -1; // in case we move outside the range defaults to last symbol


        for (int i = 0; i < weights.length; i++){
            counter += weights[i];
            if (randomNumber < counter){
                weightIndex =  i;
                break;
            }
        }

        Symbol symbol = Symbol.values()[weightIndex];
        return symbol;
    }


    /**
     * Fills the board for a new spin: every cell that is NOT sticky gets a new symbol.
     * Sticky cells (board.isSticky) keep their wild.
     */


    public void fillGrid(Board board, Random rng) {
        for (int row = 0; row < board.rowCount; row++){
            for (int reel = 0; reel < board.reelCount; reel++){
                if (board.isSticky(reel,row)){
                    continue;
                }
                Symbol symbol = drawSymbol(reel,rng);
                board.set(reel,row,symbol);
            }
        }

    }

    // ================================================================ 2. what pays
    /**
     * Every ways win on the current board, biggest first (the screen shows them in this order).
     * For each win, build a Win with: the symbol, how many reels in a row matched, the number of ways,
     * the pay per way (from config.pays), the total pay as a multiple of the bet, and the cells
     * that are part of it (cells[reel][row] = true). Empty list = no win.
     */
    public List<Win> findWins(Board board) {
        List<Win> wins = new ArrayList<>();

        for (Symbol symbol : Symbol.values()){
            double[] pays = config.pays(symbol);
            if (pays == null){
               continue;
            }

            List<Integer> ways = new ArrayList<>();
            int reelCount = 0;
            boolean[][] cells = new boolean[board.reelCount][board.rowCount];


            for (int reel = 0; reel < board.reelCount; reel++){
               int symbolCounter = 0;
               for (int row = 0; row < board.rowCount; row++){
                   if (symbol == board.get(reel,row) || board.get(reel,row) == Symbol.WILD){
                       symbolCounter += 1;
                       cells[reel][row] = true;

                   }

               }
               if (symbolCounter == 0 ){
                   break;
               }
               reelCount++;
               ways.add(symbolCounter);

           }

            if (reelCount >= MIN_WIN_LENGTH){
                int waysMultiplier = 1;

                for (int way : ways) {
                    waysMultiplier *= way;
                }
                double paysMultiplier = pays[reelCount-MIN_WIN_LENGTH];
                double totalPay = paysMultiplier*waysMultiplier;
                Win win = new Win(
                        symbol,
                        reelCount,
                        waysMultiplier,
                        paysMultiplier,
                        totalPay,
                        cells
                );
                wins.add(win);
                }
            }
        wins.sort((a, b) -> Double.compare(b.totalPay, a.totalPay));
        return wins;
    }

    /** The cells the avalanche removes after these wins ([reel][row] = true means remove). */
    public boolean[][] cellsToRemove(Board board, List<Win> wins) {
        boolean[][] cells = new boolean[board.reelCount][board.rowCount];
        for (Win win : wins){
            for (int reel = 0; reel < board.reelCount; reel++){
                for (int row = 0;row < board.rowCount; row++){
                    if (win.includes(reel,row)){
                        cells[reel][row] = true;
                    }


                }
            }
        }
        return cells;

    }

    // ================================================================ 3. avalanche

    /**
     * Removes the marked cells, lets the symbols above fall down, and fills the gaps at the top
     * with new symbols. Sticky wilds that are NOT removed stay exactly where they are.
     * A removed sticky wild is used up: unlock it (board.setSticky(..., false)).
     *
     * @return newCells[reel][row] = true for every cell that received a NEW symbol
     *         (the screen uses it to animate the drop)
     */
    public boolean[][] avalanche(Board board, boolean[][] removed, Random rng) {
        throw notImplemented("avalanche");
    }

    // ================================================================ 4. free spins

    /** How many scatters are on this grid. */
    public int countScatters(Symbol[][] grid) {
        int scatterCount = 0;
        for (int reel = 0; reel < grid.length; reel++) {
            for (int row = 0; row < grid[reel].length; row++) {
                Symbol cell = grid[reel][row];
                if (cell == Symbol.SCATTER){
                    scatterCount++;
                }
            }
        }
        return scatterCount;
    }



    /** Free spins awarded for this many scatters at the end of a base spin. 0 = no bonus. */
    public int freeSpinsAwarded(int scatterCount) {
        throw notImplemented("freeSpinsAwarded");
    }

    /** True if this many scatters trigger SUPER free spins instead of regular ones. */
    public boolean isSuperTrigger(int scatterCount) {
        throw notImplemented("isSuperTrigger");
    }

    /** Extra spins for this many scatters at the end of a FREE spin. 0 = no retrigger. */
    public int retriggerSpins(int scatterCount) {
        throw notImplemented("retriggerSpins");
    }

    /** The multiplier at the start of a bonus. */
    public int startMultiplier() {
        throw notImplemented("startMultiplier");
    }

    /** The multiplier after a winning avalanche step during free spins (it persists between spins). */
    public int nextMultiplier(int current) {
        throw notImplemented("nextMultiplier");
    }

    /** Super free spins: lock the wilds currently on the board (board.setSticky). */
    public void lockStickyWilds(Board board) {
        throw notImplemented("lockStickyWilds");
    }

    // ================================================================ 5. shop

    /** Price of one round in this mode, as a multiple of the bet. A normal spin always costs 1 bet. */
    public double price(GameMode mode) {
        if (mode == GameMode.NORMAL) {
            return 1.0;
        }
        throw notImplemented("price(" + mode + ")");
    }

    /** Free spins awarded by a feature buy (BUY_FREE_SPINS / BUY_SUPER_FREE_SPINS). */
    public int boughtFreeSpins(GameMode mode) {
        throw notImplemented("boughtFreeSpins");
    }

    /**
     * Booster effect on a freshly filled base-game grid (BOOST_SCATTER / BOOST_WILD).
     * Called for every base spin: for NORMAL (or any non-booster mode) it must do nothing.
     */
    public void applyBooster(Board board, GameMode mode, Random rng) {
        throw notImplemented("applyBooster");
    }

    // ================================================================ 6. statistics (used by the simulator)

    /** Return to player: total won / total bet. */
    public double rtp(double totalWin, double totalBet) {
        throw notImplemented("rtp");
    }

    /** Variance of the values, from their count n, their sum and the sum of their squares. */
    public double variance(long n, double sum, double sumOfSquares) {
        throw notImplemented("variance");
    }

    /** Standard deviation from a variance. */
    public double standardDeviation(double variance) {
        throw notImplemented("standardDeviation");
    }

    /** Median of values that are already sorted from low to high. */
    public double median(double[] sorted) {
        throw notImplemented("median");
    }

    /**
     * The +/- margin on a measured RTP at 95% confidence, for n rounds with this standard deviation
     * (per round, in bets) and this price per round (in bets).
     */
    public double rtpMargin95(long n, double standardDeviation, double price) {
        throw notImplemented("rtpMargin95");
    }

    /** The price a mode should cost so it returns TARGET_RTP, given its average win per round (in bets). */
    public double fairPrice(double averageWin) {
        throw notImplemented("fairPrice");
    }

    // ================================================================

    private static UnsupportedOperationException notImplemented(String what) {
        return new UnsupportedOperationException("TODO SlotMath." + what + " is not implemented yet");
    }
}