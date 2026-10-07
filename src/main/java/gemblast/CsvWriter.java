package gemblast;

import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;

public class CsvWriter {
    public void writeSimResult(Map<String, Map<String, Object>> data) {
        Path outputFolder = Path.of("output");

        try {
            Files.createDirectories(outputFolder);

            try (FileWriter writer = new FileWriter("output/spin_results.csv")) {
                writer.write(data + "\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
