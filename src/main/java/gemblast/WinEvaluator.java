package gemblast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds every ways win on a board. Pure logic: the game AND the simulator call this,
 * so what the player sees and what the statistics measure are always the same thing.
 */
public final class WinEvaluator {

    private WinEvaluator() { }

    /**
     * For each paying symbol, walk the reels left to right:
     *   count the cells on this reel that show the symbol or a wild;
     *   if zero, stop; otherwise multiply that count into the number of ways.
     * If the symbol reached 3+ reels, it's a win.
     *
     * Returned biggest win first (the order the screen shows them in).
     */
    public static List<Win> evaluate(Board board, Paytable paytable) {
        List<Win> wins = new ArrayList<>();

        for (Symbol symbol : Symbol.values()) {
            if (!paytable.isPaying(symbol)) {
                continue;                              // wild and scatter never pay on ways
            }

            int ways = 1;
            int reelsMatched = 0;
            for (int reel = 0; reel < board.reelCount; reel++) {
                int matches = countMatches(board, reel, symbol);
                if (matches == 0) {
                    break;                             // the chain is broken
                }
                ways *= matches;
                reelsMatched++;
            }

            double pay = paytable.pay(symbol, reelsMatched);   // 0 if fewer than 3 reels
            if (pay > 0) {
                wins.add(new Win(symbol, reelsMatched, ways, pay, markCells(board, symbol, reelsMatched)));
            }
        }

        // Win::multiplier is a method reference: "call multiplier() on each Win to compare them".
        wins.sort(Comparator.comparingDouble(Win::multiplier).reversed());
        return wins;
    }

    /** Sum of all wins as a multiple of the bet (handy for the simulator). */
    public static double totalMultiplier(List<Win> wins) {
        double total = 0.0;
        for (Win win : wins) {
            total += win.multiplier();
        }
        return total;
    }

    /** Every cell that is part of ANY win: these are the cells the avalanche removes. */
    public static boolean[][] winningCells(List<Win> wins, int reelCount, int rowCount) {
        boolean[][] cells = new boolean[reelCount][rowCount];
        for (Win win : wins) {
            for (int reel = 0; reel < reelCount; reel++) {
                for (int row = 0; row < rowCount; row++) {
                    if (win.includes(reel, row)) {
                        cells[reel][row] = true;
                    }
                }
            }
        }
        return cells;
    }

    private static boolean matches(Symbol onBoard, Symbol wanted) {
        return onBoard == wanted || onBoard == Symbol.WILD;
    }

    private static int countMatches(Board board, int reel, Symbol symbol) {
        int count = 0;
        for (int row = 0; row < board.rowCount; row++) {
            if (matches(board.get(reel, row), symbol)) {
                count++;
            }
        }
        return count;
    }

    private static boolean[][] markCells(Board board, Symbol symbol, int reelsMatched) {
        boolean[][] cells = new boolean[board.reelCount][board.rowCount];   // all false by default
        for (int reel = 0; reel < reelsMatched; reel++) {
            for (int row = 0; row < board.rowCount; row++) {
                cells[reel][row] = matches(board.get(reel, row), symbol);
            }
        }
        return cells;
    }
}