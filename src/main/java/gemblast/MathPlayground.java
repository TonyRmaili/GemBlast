package gemblast;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.List;

/** Scratch file for testing SlotMath without the game. Click the green arrow next to main to run it. */
public class MathPlayground {

    public static void main(String[] args) {
        ValueConfig config = ValueConfig.load();     // reads weights.json from the project root
        SlotMath math = new SlotMath(config);
        Random rng = new Random(44);                 // fixed seed = same results every run
        Board board = new Board(5,3);


        math.drawSymbol(0,rng);
        math.drawSymbol(1,rng);

        // new feature testing uniqueGemsInRow()
//        int rowIndex = 0;
//        board.set(0,0,Symbol.BLACK_DIAMOND);
//        board.setSticky(0,0,true);
//
//        board.set(1,0,Symbol.RUBY);
//        board.setSticky(1,0,true);
//
//        board.set(2,0,Symbol.SAPPHIRE);
//        board.setSticky(2,0,true);
//
//        board.set(3,0,Symbol.EMERALD);
//        board.setSticky(3,0,true);
//
//        board.set(4,0,Symbol.OPAL);
//        board.setSticky(4,0,true);
//
//        math.fillGrid(board,rng);
//        System.out.println(board);
//
//
//
//        double highest = math.uniqueGemsInRowPay(board,rowIndex);
//        System.out.println(highest);



//        double[] limits = {0, 1, 5, 20, 100, 1000};
//        double[] WINS = {0, 0.5, 1.0, 3.0, 999, 1000};
//
//        // board setup

//        board.set(2,1,Symbol.WILD);
//        board.setSticky(2,1,true);
////
//        board.set(4,2,Symbol.SCATTER);
//        board.setSticky(4,2,true);



        // method testing from SlotsMath
//        math.fillGrid(board,rng);
//        System.out.println(board);
//
//        List<Win> wins = math.findWins(board);
//        System.out.println(wins);
//
//        boolean[][] remove = math.cellsToRemove(board, wins);
//        System.out.println(Arrays.deepToString(remove));
//
//        Map<Symbol, Integer>  collectedSymbols = math.shatterCollect(board,remove);
//        System.out.println(collectedSymbols);

//        Symbol[][] grid = board.snapshot();
//        int scatterCount = math.countScatters(grid);
//        System.out.println("scatters: " + scatterCount);
//
//        boolean[][] newCells = math.avalanche(board, remove, rng);
//        System.out.println(board);
//        System.out.println(Arrays.deepToString(newCells));
//
//        for (int i = 0; i < WINS.length; i++){
//
//            int bucket = math.distributionBucket(WINS[i],limits);
//            System.out.println(bucket);
//        }


    }
}