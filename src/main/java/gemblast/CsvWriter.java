package gemblast;


import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Files;

public class CsvWriter {
    public void writeSpinResult(double payout) {
        Path outputFolder = Path.of("output");

        try {
            Files.createDirectories(outputFolder);

            try (FileWriter writer = new FileWriter("output/spin_results.csv")) {
                writer.write("payout\n");
                writer.write(payout + "\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
