package cz.crcs.sekan.rsakeysanalysis.tools;

import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 20.06.2016
 */
public class BatchesHistogram {
    public static void run(String infile, String outfile) {
        Map<String, Long> map = new TreeMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(infile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Unique keys;")) {
                    String id = line.split(";", 2)[1];
                    map.putIfAbsent(id, 0L);
                    map.put(id, map.get(id) + Long.valueOf(id));
                }
            }
        }
        catch (IOException ex) {
            System.err.println("Error while reading from file '" + infile + "': " + ex.getMessage());
        }

        try (ExtendedWriter writer = new ExtendedWriter(outfile)) {
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                writer.writeln(entry.getKey() + ";" + entry.getValue());
            }
        }
        catch (IOException ex) {
            System.err.println("Error while writing to file '" + outfile + "': " + ex.getMessage());
        }
    }
}
