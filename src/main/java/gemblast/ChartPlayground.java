package gemblast;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;



public class ChartPlayground {


    public static void main(String[] args) throws Exception {
        ValueConfig config = ValueConfig.load();
        SlotMath math = new SlotMath(config);
        Map<Integer, Double> winDistribution = new LinkedHashMap<>();
        winDistribution.put(0,        0.5573519);
        winDistribution.put(1,      0.2893248);
        winDistribution.put(5,      0.1300994);
        winDistribution.put(20,     0.0158234);
        winDistribution.put(100,   0.0062887);
        winDistribution.put(1000, 0.0011114);
        winDistribution.put(10000,    4.0E-7);

        System.out.println( math.histogramEdge(10));
        System.out.print( math.histogramBin(50000));

//        double bins = math.totalBins();
//        System.out.print(bins);
    }
}