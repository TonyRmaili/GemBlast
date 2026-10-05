package gemblast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Plays complete rounds. It only decides the ORDER of things (spin, wins, avalanche, free spins);
 * every decision about symbols, pays and rules is a call to SlotMath.
 *
 * Main calls it for one round and animates the result; the simulator calls it millions of times.
 * Same code, same maths.
 */
public final class GameEngine {

    private final SlotMath math;
    private final Board board;

    public GameEngine(SlotMath math, int reelCount, int rowCount) {
        this.math = math;
        this.board = new Board(reelCount, rowCount);
    }

    /**
     * One paid round in the given mode:
     *   NORMAL / boosters: a base spin, then free spins if SlotMath says the scatters trigger them
     *   buys:              straight into the bought bonus, no base spin
     */
    public RoundResult playRound(Random rng, GameMode mode) {
        board.clearSticky();

        if (mode.isBuy()) {
            boolean superMode = (mode == GameMode.BUY_SUPER_FREE_SPINS);
            return new RoundResult(mode, null, playFreeSpins(rng, math.boughtFreeSpins(mode), superMode));
        }

        SpinResult baseSpin = playSpin(rng, false, false, mode);

        FreeSpinsResult freeSpins = null;
        int awarded = math.freeSpinsAwarded(baseSpin.scatterCount);
        if (awarded > 0) {
            freeSpins = playFreeSpins(rng, awarded, math.isSuperTrigger(baseSpin.scatterCount));
        }
        return new RoundResult(mode, baseSpin, freeSpins);
    }

    /**
     * The whole bonus. Three things persist from spin to spin:
     *   - the multiplier: a local variable passed into every spin
     *   - the SHATTER meters: an array passed into every spin, which fills it and empties it on explosions
     *   - (super only) the sticky wilds: stored in the board, which is not cleared between free spins
     */
    private FreeSpinsResult playFreeSpins(Random rng, int spinsAwarded, boolean superMode) {
        board.clearSticky();
        int spinsLeft = spinsAwarded;
        int startMultiplier = math.startMultiplier();
        int multiplier = startMultiplier;
        int[] shatterMeters = new int[Symbol.values().length];   // one meter per gem, by ordinal; all start at 0
        List<SpinResult> spins = new ArrayList<>();
        List<Integer> extraSpins = new ArrayList<>();

        while (spinsLeft > 0 && spins.size() < SlotMath.MAX_FREE_SPINS) {
            spinsLeft--;
            SpinResult spin = playSpin(rng, true, superMode, GameMode.NORMAL, multiplier, shatterMeters);
            multiplier = spin.multiplierAfter;                 // carried into the next spin

            int extra = math.retriggerSpins(spin.scatterCount);
            spinsLeft += extra;
            spins.add(spin);
            extraSpins.add(extra);
        }
        board.clearSticky();                                   // bonus over: unlock everything
        return new FreeSpinsResult(superMode, spinsAwarded, startMultiplier, spins, extraSpins);
    }

    /** A base-game spin: the multiplier is 1 and never grows, and there are no SHATTER meters. */
    private SpinResult playSpin(Random rng, boolean freeSpin, boolean stickyWilds, GameMode booster) {
        return playSpin(rng, freeSpin, stickyWilds, booster, 1, null);
    }

    /**
     * One spin: fill the board, then keep avalanching while there are wins.
     * @param freeSpin    true = the multiplier grows after every winning step (SlotMath.nextMultiplier)
     * @param stickyWilds super free spins: lock wilds after the grid lands and after every refill,
     *                    and check the row BONUS on the final grid
     * @param booster     the mode, so SlotMath can apply a booster to the first grid
     * @param multiplier  multiplier applied to the first win step
     * @param shatterMeters free spins: the SHATTER meters (filled during the spin, emptied by explosions
     *                      at its end); null in the base game = feature off
     */
    private SpinResult playSpin(Random rng, boolean freeSpin, boolean stickyWilds, GameMode booster, int multiplier,
                                int[] shatterMeters) {
        boolean[][] stickyBefore = board.stickySnapshot();
        int[] metersBefore = shatterMeters == null ? null : shatterMeters.clone();
        math.fillGrid(board, rng);
        if (!freeSpin) {
            math.applyBooster(board, booster, rng);
        }
        Symbol[][] initialGrid = board.snapshot();
        if (stickyWilds) {
            math.lockStickyWilds(board);
        }
        boolean[][] stickyLanded = board.stickySnapshot();

        List<SpinResult.Cascade> cascades = new ArrayList<>();
        for (int step = 0; step < SlotMath.MAX_CASCADES; step++) {
            List<Win> wins = math.findWins(board);
            if (wins.isEmpty()) {
                break;                                           // no win = the chain ends
            }
            boolean[][] removed = math.cellsToRemove(board, wins);

            // SHATTER: collect the winning gems NOW, while they are still on the board (avalanche removes them).
            Map<Symbol, Integer> shattered = Collections.emptyMap();
            if (shatterMeters != null) {
                shattered = math.shatterCollect(board, removed);
                for (Map.Entry<Symbol, Integer> entry : shattered.entrySet()) {
                    shatterMeters[entry.getKey().ordinal()] += entry.getValue();
                }
            }

            boolean[][] newCells = math.avalanche(board, removed, rng);
            if (stickyWilds) {
                math.lockStickyWilds(board);
            }
            int multiplierAfter = freeSpin ? math.nextMultiplier(multiplier) : multiplier;
            cascades.add(new SpinResult.Cascade(wins, removed, board.snapshot(), newCells,
                    board.stickySnapshot(), multiplier, multiplierAfter, shattered));
            multiplier = multiplierAfter;
        }
        // Row BONUS: super free spins only, checked once on the final grid, after every avalanche.
        double[] rowBonus = new double[board.rowCount];       // all 0 = no row bonus
        if (stickyWilds) {
            for (int row = 0; row < board.rowCount; row++) {
                rowBonus[row] = math.uniqueGemsInRowPay(board, row);
            }
        }

        // SHATTER: at the very end of the spin, every full meter explodes (with the end-of-spin multiplier).
        // The extra above the meter size is kept, so no collected gem is ever lost.
        List<Symbol> shatterGems = new ArrayList<>();
        List<Double> shatterPays = new ArrayList<>();
        if (shatterMeters != null) {
            for (Symbol gem : Symbol.values()) {
                while (shatterMeters[gem.ordinal()] >= SlotMath.SHATTER_METER_SIZE) {
                    shatterMeters[gem.ordinal()] -= SlotMath.SHATTER_METER_SIZE;
                    shatterGems.add(gem);
                    shatterPays.add(math.shatterPay(gem, multiplier));
                }
            }
        }

        int scatters = math.countScatters(board.snapshot());
        return new SpinResult(initialGrid, stickyBefore, stickyLanded, cascades, multiplier, scatters, rowBonus,
                metersBefore, shatterMeters == null ? null : shatterMeters.clone(), shatterGems, shatterPays);
    }
}