package gemblast;

import java.util.*;
import java.util.HashMap;
import java.util.Map;

public final class SlotMath {

    // ================================================================ rules (design choices, change freely)

    /** Wins need at least this many adjacent reels from the left. Also used by the pay-table display. */
    public static final int MIN_WIN_LENGTH = 3;

    /** Return target used for fair shop prices and in the simulator report. */
    public static final double TARGET_RTP = 0.96;

    /** Safety limits so a loop can never run forever. Not part of the maths; leave them high. */
    public static final int MAX_CASCADES = 100;
    public static final int MAX_FREE_SPINS = 200;

    // free spin constants
    public static final int FREE_SPINS_AWARDED = 10;
    public static final int RETRIGGER_SPINS = 5;
    public static final int FREE_SPINS_SCATTERS  = 3;
    public static final int SUPER_SCATTERS  = 4;
    public static final int RETRIGGER_SCATTERS  = 3;
    public static final int MULTIPLIER_START  = 1;

    public static final int SHATTER_METER_SIZE = 10;
    private final ValueConfig config;

    public SlotMath(ValueConfig config) {
        this.config = config;
    }

    // ================================================================ 1. how symbols land

    /**
     * One random symbol for one cell on the given reel (0 = leftmost), drawn with that reel's
     * weights from config.weights(reel). Weight w out of a reel total T means probability w / T.
     */
    public Symbol drawSymbol(int reel, Random rng) {
        double[] weights = config.weights(reel);
        double randomNumber = rng.nextDouble();

        double totalWeight = 0;
        for (double weight : weights){
            totalWeight += weight;
        }

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
        return Symbol.values()[weightIndex];
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
        boolean[][] newCells = new boolean[board.reelCount][board.rowCount];

        for (int reel = 0; reel < board.reelCount; reel++ ){
            List<Symbol> survivors = new ArrayList<>();
            for (int row = board.rowCount - 1; row >= 0; row--){
                // finds the cells that needs removal
                if (!removed[reel][row] && !board.isSticky(reel,row)){
                    survivors.add(board.get(reel,row));
                }
                if (removed[reel][row] && board.isSticky(reel,row)){
                    board.setSticky(reel,row,false);
                }

            }
            int next = 0;
            for (int row = board.rowCount - 1; row >= 0; row--){
                if (board.isSticky(reel,row)){
                    continue;
                }
                if (next < survivors.size()){
                    board.set(reel,row,survivors.get(next));
                    next++;
                }
                else{
                    board.set(reel,row,drawSymbol(reel,rng));
                    newCells[reel][row] = true;
                }
            }
        }
        return newCells;
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
        if (scatterCount >= FREE_SPINS_SCATTERS){
            return FREE_SPINS_AWARDED;
        }
        return 0;
    }

    /** True if this many scatters trigger SUPER free spins instead of regular ones. */
    public boolean isSuperTrigger(int scatterCount) {
        if (scatterCount >= SUPER_SCATTERS){
            return true;
        }
        return false;
    }

    /** Extra spins for this many scatters at the end of a FREE spin. 0 = no retrigger. */
    public int retriggerSpins(int scatterCount) {
        if (scatterCount >= RETRIGGER_SCATTERS){
            return RETRIGGER_SPINS;
        }
        return 0;
    }

    /** The multiplier at the start of a bonus. */
    public int startMultiplier() {
        return MULTIPLIER_START;
    }

    /** The multiplier after a winning avalanche step during free spins (it persists between spins). */
    public int nextMultiplier(int current) {
        return current + 1;
    }

    /** Super free spins: lock the wilds currently on the board (board.setSticky). */
    public void lockStickyWilds(Board board) {
        for (int reel = 0; reel < board.reelCount; reel++) {
            for (int row = 0; row < board.rowCount; row++) {
                if (board.get(reel, row) == Symbol.WILD) {
                    board.setSticky(reel, row, true);
                }
            }
        }
    }

    /** new feature - only triggered in free spins and super free spins */
    public Map<Symbol, Integer> shatterCollect(Board board,boolean[][] removed){
        Map<Symbol, Integer> collectedSymbols = new HashMap<>();

        for (int reel = 0; reel < board.reelCount;reel++){
            for (int row = 0; row < board.rowCount; row++){
                if ( removed[reel][row] ){
                    Symbol symbol = board.get(reel,row);
                    if (config.pays(symbol) != null){
                        collectedSymbols.put(symbol, collectedSymbols.getOrDefault(symbol, 0) + 1);
                    }
                }
            }
        }
        return collectedSymbols;
    }

    /** Pay when this gem's meter fills: its 5-of-a-kind pay times the multiplier, x bet. */
    public double shatterPay(Symbol symbol, int multiplier) {
        double[] pays = config.pays(symbol);
        double highestPay = pays[pays.length-1];

        return highestPay*multiplier;
    }


    /** new feature - only triggered in super free spins */
    public double uniqueGemsInRowPay(Board board,int rowIndex){
        // place all symbols in a list, skip Wild and Scatter. finally check if they are unique
        List<Symbol> row = new ArrayList<>();
        for (int reel = 0; reel < board.reelCount; reel++){
            if (board.get(reel,rowIndex) == Symbol.WILD || board.get(reel,rowIndex) == Symbol.SCATTER){
                return 0;
            }
            row.add(board.get(reel,rowIndex));
        }
        boolean allUnique = row.stream()
                .distinct()
                .count() == row.size();

        // find the highest paying Symbol
        if (allUnique){
            List<Double> highestPays = new ArrayList<>(board.reelCount);
            for (Symbol symbol : row){
                double[] pays = config.pays(symbol);
                double highestPay = pays[pays.length-1];
                highestPays.add(highestPay);
            }
            return Collections.max(highestPays);
        }
        return 0;
    }

    // ================================================================ 5. shop

    /** Price of one round in this mode, as a multiple of the bet. A normal spin always costs 1 bet. */
    // dummy prices for now until verified rtp is ran through simulator
    public double price(GameMode mode) {
        if (mode == GameMode.NORMAL) {
            return 1.0;
        }
        else if (mode == GameMode.BOOST_SCATTER){
            return 1.43;
        }
        else if (mode == GameMode.BOOST_WILD){
            return 1.20;
        }
        else if (mode == GameMode.BUY_FREE_SPINS){
            return 30;
        }
        else if (mode == GameMode.BUY_SUPER_FREE_SPINS){
            return 115.3;
        }

        throw notImplemented("price(" + mode + ")");
    }

    /** Free spins awarded by a feature buy (BUY_FREE_SPINS / BUY_SUPER_FREE_SPINS). */
    public int boughtFreeSpins(GameMode mode) {
        if (mode == GameMode.BUY_FREE_SPINS) {
            return FREE_SPINS_AWARDED;
        }
        else if (mode == GameMode.BUY_SUPER_FREE_SPINS) {
            return FREE_SPINS_AWARDED;
        }
        throw notImplemented("boughtFreeSpins");
    }

    /**
     * Booster effect on a freshly filled base-game grid (BOOST_SCATTER / BOOST_WILD).
     * Called for every base spin: for NORMAL (or any non-booster mode) it must do nothing.
     */
    public void applyBooster(Board board, GameMode mode, Random rng) {

        if (mode == GameMode.BOOST_SCATTER){
            Symbol scatter =  Symbol.SCATTER;
            int randomRow = rng.nextInt(board.rowCount);

            board.set(board.reelCount-1,randomRow,scatter);
        }

        else if (mode == GameMode.BOOST_WILD){
            Symbol wild = Symbol.WILD;
            int randomRow = rng.nextInt(board.rowCount);
            int randomReel = rng.nextInt(board.reelCount-1) +1;
            board.set(randomReel,randomRow,wild);
        }
    }

    // ================================================================ 6. statistics (used by the simulator)
    public double rtp(double totalWin, double totalBet) {
        return totalWin / totalBet;
    }

    public double variance(long n, double sum, double sumOfSquares) {
        return Math.max(0, sumOfSquares / n - Math.pow(sum / n, 2));
    }

    public double standardDeviation(double variance) {
        return Math.sqrt(variance);
    }
    public double median(double[] sorted) {
        // check for even or odd length of sorted
        // case odd
        if (sorted.length % 2 == 1){
            int middleIndex = sorted.length/2;
            return sorted[middleIndex];
        }
        // case even
        else {
            int upperMiddleIndex = sorted.length/2;
            int lowerMiddleIndex = upperMiddleIndex -1;

            double upperMiddleNumber = sorted[upperMiddleIndex];
            double lowerMiddleNumber = sorted[lowerMiddleIndex];

            return (upperMiddleNumber+lowerMiddleNumber)/2;
        }
    }

    public double totalBet(long rounds, double price) {
        return rounds*price;
    }

    public double average(double sum, long count) {
        return sum/count;
    }

    public double frequency(long count, long total) {
        return (double) count /total;
    }

    public double oneIn(long count, long total) {
        return (double) total/count;
    }

    public int distributionBucket(double win, double[] limits) {

        for (int i = 1; i < limits.length ; i++) {
            if (win == 0) {
                return 0;
            } else if (win >= limits[i - 1] && win < limits[i]) {
                return i;
            }
        }
        return limits.length;
    }

    public double rtpMargin95(long n, double standardDeviation, double price) {
        return (1.96 * standardDeviation / Math.sqrt(n))/price;
    }
    public double fairPrice(double averageWin) {
        return averageWin/TARGET_RTP;
    }
    // ================================================================

    private static UnsupportedOperationException notImplemented(String what) {
        return new UnsupportedOperationException("TODO SlotMath." + what + " is not implemented yet");
    }
}