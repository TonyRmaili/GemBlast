package gemblast;

import java.util.*;

public class TheoreticalMath {
    ValueConfig config;
    List<Double> reel1;
    List<Double> reel2to5;
    Map<String,Map<String,List<Double>>> symbolMap;
    List<Double> weightTotals;
    int REEL_COUNT;
    int ROW_COUNT;
    int MIN_WIN_LENGTH;


    public TheoreticalMath(ValueConfig config){
        this.config = config;
        this.reel1 = config.reel1;
        this.reel2to5 = config.reel2to5;
        this.symbolMap = symbolMap();
        this.weightTotals = weightTotal();
        this.cellProbability();
        this.matchProbability();
        this.REEL_COUNT = 5;
        this.ROW_COUNT = 3;
        this.MIN_WIN_LENGTH = 3;
    }


    public Map<String, Map<String, List<Double>>> symbolMap() {

        Map<String, Map<String, List<Double>>> hashedMap = new LinkedHashMap<>();

        Symbol[] symbols = Symbol.values();

        for (int index = 0; index < symbols.length; index++) {

            Symbol symbol = symbols[index];
            String name = symbol.name();

            Map<String, List<Double>> innerMap = new LinkedHashMap<>();

            // Paytable is null for symbols without a paytable entry
            innerMap.put("paytable", config.paytable.get(name));

            // Weights are retrieved using the symbol's enum index
            List<Double> weights = new ArrayList<>();
            weights.add(config.reel1.get(index));
            weights.add(config.reel2to5.get(index));

            innerMap.put("weights", weights);

            hashedMap.put(name, innerMap);
        }

        return hashedMap;
    }


    public List<Double> weightTotal(){
        List<Double> weightTotals = new ArrayList<>();

        Double reel1Total = (double) 0;
        Double reel2to5Total = (double) 0;

        for (int i = 0; i < reel1.size(); i++){
            reel1Total += reel1.get(i);
            reel2to5Total += reel2to5.get(i);

        }

        weightTotals.add(reel1Total);
        weightTotals.add(reel2to5Total);

        return weightTotals;

    }

    public void cellProbability(){
        for (Symbol symbol : Symbol.values()){
            List<Double> probabilities = new ArrayList<>();

            double pReel1 = symbolMap.get(symbol.toString()).get("weights").get(0) / weightTotals.get(0);
            double pReel2to5 = symbolMap.get(symbol.toString()).get("weights").get(1) / weightTotals.get(1);

            probabilities.add(pReel1);
            probabilities.add(pReel2to5);

            symbolMap.get(symbol.toString()).put("cellProbability", probabilities);
        }
    }

    public void matchProbability(){
        List<Double> wildP = symbolMap.get("WILD").get("cellProbability");

        for (Symbol symbol : Symbol.values()){
            List<Double> probabilities = new ArrayList<>();
            if (symbol == Symbol.WILD || symbol == Symbol.SCATTER){
                continue;
            }
            probabilities.add(symbolMap.get(symbol.toString()).get("cellProbability").get(0) + wildP.get(0));
            probabilities.add(symbolMap.get(symbol.toString()).get("cellProbability").get(1) + wildP.get(1));
            symbolMap.get(symbol.toString()).put("matchProbability", probabilities);

        }
    }

    public double expectedWays(int k, String symbol ){
        double ways = 1;
        List<Double> matchProbability = symbolMap.get(symbol).get("matchProbability");

        double p1 = matchProbability.get(0);
        double p2to5 = matchProbability.get(1);

        for (int i = 0; i < k; i++){
            double p = (i == 0) ? p1 : p2to5;

            ways *= ROW_COUNT * p;
        }

        if (k < REEL_COUNT) {
            ways *= Math.pow(1 - p2to5, ROW_COUNT);
        }

        return ways;
    }

    public double symbolEV(String symbol){
        double EV = 0;
        List<Double> paytable = symbolMap.get(symbol).get("paytable");


        for (int k = MIN_WIN_LENGTH; k <= REEL_COUNT; k++){

            int index = k - MIN_WIN_LENGTH;
            EV += paytable.get(index) * expectedWays(k,symbol);
        }
        return EV;
    }

    public double firstGridRTP(){
        double total = 0;

        for (Symbol symbol : Symbol.values()){
            if (symbol == Symbol.WILD || symbol == Symbol.SCATTER){
                continue;
            }
            total += symbolEV(symbol.toString());

        }
        return total;

    }
}

