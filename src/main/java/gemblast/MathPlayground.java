package gemblast;

import java.util.*;

/** Scratch file for testing SlotMath without the game. Click the green arrow next to main to run it. */
public class MathPlayground {
    // System.out.println();
    public static void main(String[] args) {
        ValueConfig config = ValueConfig.load();
        TheoreticalMath theoryMath = new TheoreticalMath(config);
        SlotMath slotMath = new SlotMath(config);
        int SEED = 42;
        Random rng = new Random();
        Board board = new Board(5,3);

        //---------------- SLOT MATH ---------------------------------------------------------


//        int reel = 1;
//        Symbol drawnSymbol = slotMath.drawSymbol(reel,rng);
//        System.out.println("draw one symbol: " + drawnSymbol);
//
//
//
//
//        slotMath.fillGrid(board,rng);
//        System.out.println("---------Filled grid------");
//        System.out.println(board);
//        System.out.println("--------------------------");
//
//
//        List<Win> wins = slotMath.findWins(board);
//        System.out.println("wins: "+ wins);
//
//        boolean[][] removableCells = slotMath.cellsToRemove(board,wins);
//        System.out.println("---------cells to remove-------");
//        System.out.println(Arrays.deepToString(removableCells));
//        System.out.println("-------------------------------");
//
//
//        System.out.println("---------avalanche-------");
//        boolean[][] newGrid = slotMath.avalanche(board,removableCells,rng);
//        System.out.println(Arrays.deepToString(newGrid));
//        System.out.println("-------------------------");


//        board.set(4,1,Symbol.SCATTER);
//        board.setSticky(4,1,true);
////             more stickys
//        board.set(4,0,Symbol.SCATTER);
//        board.setSticky(4,0,true);
//
//        board.set(4,2,Symbol.SCATTER);
//        board.setSticky(4,2,true);
//
//        board.set(3,1,Symbol.SCATTER);
//        board.setSticky(3,1,true);


//        System.out.println("---------Grid with scatter------");
//        System.out.println(board);
//
//        int totalScatters = slotMath.countScatters(board.snapshot());
//        int freeSpins = slotMath.freeSpinsAwarded(totalScatters);
//        boolean superTriggered = slotMath.isSuperTrigger(totalScatters);
//
//        System.out.println("totalScatters: " + totalScatters);
//        System.out.println("free spins triggered: " + freeSpins);
//        System.out.println("is it super free spins?: " + superTriggered);
//        System.out.println("--------------------------");



//
//        Map<Symbol, Integer> collectedSymbols = slotMath.shatterCollect(board,removableCells);
//        System.out.println("collectedSymbols: " + collectedSymbols);
//
//
//        // fixed shatter demo per symbol
//        Symbol symbol = Symbol.RUBY;
//        int endMultiplier = 3;
//        double paidShatter = slotMath.shatterPay(symbol,endMultiplier);
//        System.out.println(symbol + " shattered with multipier " + endMultiplier + " and won: " + paidShatter);
//        System.out.println("--------------------------");
//
//        System.out.println("----------Unique Row----------------");
//        for (int rowIndex = 0; rowIndex < board.rowCount; rowIndex++){
//            double payingRow = slotMath.uniqueGemsInRowPay(board,rowIndex);
//            System.out.println("Row " + (rowIndex +1) + " paid: " + payingRow);
//
//        }



        // ---------------------THEORETICAL MATH ------------------------------

        // avalanche factor from sim : 1.35x
        System.out.println("first grid RTP: " + theoryMath.firstGridRTP()*100 +"%");

        System.out.println("symbol Map " + theoryMath.symbolMap);


        // free spin trigger
        double freeSpins = theoryMath.probabilityAtLeastOnGrid(Symbol.SCATTER,3);

        // super free spin trigger
        double superFree = theoryMath.probabilityAtLeastOnGrid(Symbol.SCATTER,4);

        System.out.printf("Free spins:       %.4f%%   (1 in %,.0f)%n", freeSpins * 100, 1 / freeSpins);
        System.out.printf("Super free spins: %.4f%%   (1 in %,.0f)%n", superFree * 100, 1 / superFree);

        // compare with simulator value; 10m spins normal mode => Win frequency per round 43.68%
        System.out.printf("any win probability: %.4f%%%n", theoryMath.anyWinProbability() * 100);



    }
}