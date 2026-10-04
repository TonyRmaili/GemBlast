package gemblast;

import java.util.Arrays;
import java.util.Random;
import java.util.List;

/** Scratch file for testing SlotMath without the game. Click the green arrow next to main to run it. */
public class MathPlayground {

    public static void main(String[] args) {
        ValueConfig config = ValueConfig.load();     // reads weights.json from the project root
        SlotMath math = new SlotMath(config);
        Random rng = new Random(42);                 // fixed seed = same results every run

        // board setup
        Board board = new Board(5,3);
        board.set(2,1,Symbol.WILD);
        board.setSticky(2,1,true);

        // method testing from SlotsMath
        math.fillGrid(board,rng);
        System.out.println(board);

        List<Win> wins = math.findWins(board);
        System.out.println(wins);

        boolean[][] remove = math.cellsToRemove(board, wins);
        System.out.println(Arrays.deepToString(remove));


    }
}