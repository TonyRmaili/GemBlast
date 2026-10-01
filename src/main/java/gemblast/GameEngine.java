package gemblast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plays complete rounds. This is THE game logic: Main calls it for one round and animates the result;
 * the simulator calls it millions of times and collects statistics. Same code, same maths.
 */
public final class GameEngine {

    private final ReelWeights weights;
    private final Paytable paytable;
    private final Board board;

    public GameEngine(ReelWeights weights, Paytable paytable, int reelCount, int rowCount) {
        this.weights = weights;
        this.paytable = paytable;
        this.board = new Board(reelCount, rowCount);
    }

    /** A normal paid round. */
    public RoundResult playRound(Random rng) {
        return playRound(rng, GameMode.NORMAL);
    }

    /**
     * One paid round in the given mode:
     *   NORMAL / boosters: a base spin (boosted if a booster is on), then free spins if enough scatters
     *   buys:              straight into the bought bonus, no base spin
     * (Two methods with the same name but different parameters is called "overloading".)
     */
    public RoundResult playRound(Random rng, GameMode mode) {
        board.clearSticky();                                   // nothing is ever sticky in the base game

        if (mode == GameMode.BUY_FREE_SPINS) {
            return new RoundResult(mode, null, playFreeSpins(rng, false));
        }
        if (mode == GameMode.BUY_SUPER_FREE_SPINS) {
            return new RoundResult(mode, null, playFreeSpins(rng, true));
        }

        SpinResult baseSpin = playSpin(rng, 1, 0, false, mode);    // base game: x1, never grows

        FreeSpinsResult freeSpins = null;
        int scatters = baseSpin.scatterCount();
        if (scatters >= GameRules.FREE_SPINS_SCATTERS) {
            freeSpins = playFreeSpins(rng, scatters >= GameRules.SUPER_SCATTERS);
        }
        return new RoundResult(mode, baseSpin, freeSpins);
    }

    /**
     * The whole bonus. Two things persist from spin to spin:
     *   - the multiplier: a local variable passed into every spin
     *   - (super only) the sticky wilds: stored in the board, which is not cleared between free spins
     */
    private FreeSpinsResult playFreeSpins(Random rng, boolean superMode) {
        board.clearSticky();
        int spinsLeft = GameRules.FREE_SPINS_AWARDED;
        int multiplier = GameRules.MULTIPLIER_START;
        List<SpinResult> spins = new ArrayList<>();

        while (spinsLeft > 0 && spins.size() < GameRules.MAX_FREE_SPINS) {
            spinsLeft--;
            SpinResult spin = playSpin(rng, multiplier, GameRules.MULTIPLIER_STEP, superMode, GameMode.NORMAL);
            multiplier = spin.multiplierAfter;                 // carried into the next spin

            if (spin.scatterCount() >= GameRules.RETRIGGER_SCATTERS) {
                spinsLeft += GameRules.RETRIGGER_SPINS;
            }
            spins.add(spin);
        }
        board.clearSticky();                                   // bonus over: unlock everything
        return new FreeSpinsResult(superMode, spins);
    }

    /**
     * One spin: spin the board, then keep avalanching while there are wins.
     * @param multiplier     multiplier applied to the first win step
     * @param multiplierStep how much it grows after each winning step (0 = never grows)
     * @param stickyWilds    super free spins: every wild that shows up locks until it is part of a win
     * @param booster        BOOST_SCATTER / BOOST_WILD force one symbol onto the first grid; anything else: no effect
     */
    private SpinResult playSpin(Random rng, int multiplier, int multiplierStep, boolean stickyWilds, GameMode booster) {
        boolean[][] stickyBefore = board.stickySnapshot();
        board.spin(weights, rng);                              // sticky cells keep their wild
        applyBooster(booster, rng);
        Symbol[][] initialGrid = board.snapshot();
        if (stickyWilds) {
            board.lockWilds(GameRules.MAX_STICKY_WILDS);
        }
        boolean[][] stickyLanded = board.stickySnapshot();

        List<SpinResult.Cascade> cascades = new ArrayList<>();
        for (int step = 0; step < GameRules.MAX_CASCADES; step++) {
            List<Win> wins = WinEvaluator.evaluate(board, paytable);
            if (wins.isEmpty()) {
                break;                                           // no win = the chain ends
            }
            // Winning wilds are removed like any winning symbol (sticky ones are used up), so every chain ends.
            boolean[][] removed = WinEvaluator.winningCells(wins, board.reelCount, board.rowCount);
            boolean[][] newCells = board.avalanche(removed, weights, rng);
            if (stickyWilds) {
                board.lockWilds(GameRules.MAX_STICKY_WILDS);   // wilds that dropped in lock too
            }
            int multiplierAfter = multiplier + multiplierStep;
            cascades.add(new SpinResult.Cascade(wins, removed, board.snapshot(), newCells,
                    board.stickySnapshot(), multiplier, multiplierAfter));
            multiplier = multiplierAfter;
        }
        return new SpinResult(initialGrid, stickyBefore, stickyLanded, cascades, multiplier);
    }

    /** Overwrites one random cell after the normal spin, so the rest of the grid keeps its usual odds. */
    private void applyBooster(GameMode booster, Random rng) {
        if (booster == GameMode.BOOST_SCATTER) {
            int lastReel = board.reelCount - 1;
            board.set(lastReel, rng.nextInt(board.rowCount), Symbol.SCATTER);
        } else if (booster == GameMode.BOOST_WILD) {
            int reel = 1 + rng.nextInt(board.reelCount - 1);   // reels 2-5 (index 1-4): wilds never land on reel 1
            board.set(reel, rng.nextInt(board.rowCount), Symbol.WILD);
        }
    }
}