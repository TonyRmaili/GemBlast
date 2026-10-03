package gemblast;

import java.util.ArrayList;
import java.util.List;
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
     * The whole bonus. Two things persist from spin to spin:
     *   - the multiplier: a local variable passed into every spin
     *   - (super only) the sticky wilds: stored in the board, which is not cleared between free spins
     */
    private FreeSpinsResult playFreeSpins(Random rng, int spinsAwarded, boolean superMode) {
        board.clearSticky();
        int spinsLeft = spinsAwarded;
        int startMultiplier = math.startMultiplier();
        int multiplier = startMultiplier;
        List<SpinResult> spins = new ArrayList<>();
        List<Integer> extraSpins = new ArrayList<>();

        while (spinsLeft > 0 && spins.size() < SlotMath.MAX_FREE_SPINS) {
            spinsLeft--;
            SpinResult spin = playSpin(rng, true, superMode, GameMode.NORMAL, multiplier);
            multiplier = spin.multiplierAfter;                 // carried into the next spin

            int extra = math.retriggerSpins(spin.scatterCount);
            spinsLeft += extra;
            spins.add(spin);
            extraSpins.add(extra);
        }
        board.clearSticky();                                   // bonus over: unlock everything
        return new FreeSpinsResult(superMode, spinsAwarded, startMultiplier, spins, extraSpins);
    }

    /** A base-game spin: the multiplier is 1 and never grows. */
    private SpinResult playSpin(Random rng, boolean freeSpin, boolean stickyWilds, GameMode booster) {
        return playSpin(rng, freeSpin, stickyWilds, booster, 1);
    }

    /**
     * One spin: fill the board, then keep avalanching while there are wins.
     * @param freeSpin    true = the multiplier grows after every winning step (SlotMath.nextMultiplier)
     * @param stickyWilds super free spins: lock wilds after the grid lands and after every refill
     * @param booster     the mode, so SlotMath can apply a booster to the first grid
     * @param multiplier  multiplier applied to the first win step
     */
    private SpinResult playSpin(Random rng, boolean freeSpin, boolean stickyWilds, GameMode booster, int multiplier) {
        boolean[][] stickyBefore = board.stickySnapshot();
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
            boolean[][] newCells = math.avalanche(board, removed, rng);
            if (stickyWilds) {
                math.lockStickyWilds(board);
            }
            int multiplierAfter = freeSpin ? math.nextMultiplier(multiplier) : multiplier;
            cascades.add(new SpinResult.Cascade(wins, removed, board.snapshot(), newCells,
                    board.stickySnapshot(), multiplier, multiplierAfter));
            multiplier = multiplierAfter;
        }
        int scatters = math.countScatters(board.snapshot());
        return new SpinResult(initialGrid, stickyBefore, stickyLanded, cascades, multiplier, scatters);
    }
}
