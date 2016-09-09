package cz.crcs.sekan.rsakeysanalysis.classification.table.makefile;

import cz.crcs.sekan.rsakeysanalysis.classification.table.RawTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.identification.IdentificationGenerator;
import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;
import cz.crcs.sekan.rsakeysanalysis.common.exception.WrongKeyException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 18.04.2016
 */
public class SourceParser extends Thread {
    /**
     * Table where results will be added
     */
    private RawTable rawTable;

    /**
     * Generator for generating key identification
     */
    private IdentificationGenerator identificationGenerator;

    /**
     * Name of source
     */
    private String source;

    /**
     * Set of directories where to find csv files
     */
    private Set<String> directories;

    public SourceParser(RawTable rawTable, IdentificationGenerator identificationGenerator, String source, Set<String> directories) {
        this.rawTable = rawTable;
        this.identificationGenerator = identificationGenerator;
        this.source = source;
        this.directories = directories;
    }

    public String getSource() {
        return source;
    }

    public void run() {
        Map<String, Long> identificationsCount = new TreeMap<>();

        for (String directoryPath : directories) {
            File directory = new File(directoryPath);
            File[] files = directory.listFiles();
            if (files == null) {
                System.err.println("Cannot read files from folder '" + directoryPath + "'");
                continue;
            }

            for (File csvFile : files) {
                if (!csvFile.getName().endsWith(".csv")) {
                    continue;
                }

                try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                    String line;
                    long lineNumber = 0, keys = 0;
                    while ((line = reader.readLine()) != null) {
                        try {
                            lineNumber++;
                            RSAKey rsaKey = RSAKey.loadFromString(line);
                            if (rsaKey == null) {
                                continue;
                            }

                            String identification = identificationGenerator.generationIdentification(rsaKey);
                            Long count = identificationsCount.getOrDefault(identification, 0L) + 1;
                            identificationsCount.put(identification, count);
                            keys++;
                        }
                        catch (WrongKeyException ex) {
                            System.err.println("Key on line " + lineNumber + " in file '" + csvFile + "' is not correct. Cause: " + ex.getCause().getMessage());
                        }
                    }
                    System.out.println(source + " | " + csvFile.getName() + " | " + keys);

                }
                catch (IOException ex) {
                    System.err.println("Exception while reading file '" + csvFile.getAbsolutePath() + "': " + ex.getMessage());
                }
            }
        }

        rawTable.addSource(source, identificationsCount);
    }
}