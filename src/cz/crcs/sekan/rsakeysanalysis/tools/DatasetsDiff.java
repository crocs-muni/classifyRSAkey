package cz.crcs.sekan.rsakeysanalysis.tools;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 02/10/2016
 */
public class DatasetsDiff {
    public static void run(String dataSetA, String dataSetB, String outFolder) {
        List<ClassificationKey> keys = new ArrayList<>();
        System.out.println("Reading keys from first dataset.");
        try (BufferedReader reader = new BufferedReader(new FileReader(dataSetA))) {
            String line;
            long lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().length() == 0) continue;
                try {
                    keys.add(new ClassificationKey(line));
                }
                catch (Exception ex) {
                    System.err.println("Cannot read key on line '" + lineNumber +"' from dataset '" + dataSetA + "'.");
                }
            }
        }
        catch (IOException ex) {
            System.err.println("Error while reading from file '" + dataSetA + "': " + ex.getMessage());
        }


        //Create folder for results if does not exist
        File folderFile = new File(outFolder);
        if (!folderFile.exists()) {
            if (!folderFile.mkdirs()) {
                throw new IllegalArgumentException("Cannot create folder.");
            }
        }

        //Check if path to folder is correctly ended
        if (!outFolder.endsWith("/") && !outFolder.endsWith("\\")) {
            outFolder += "/";
        }

        String dataSetAFileName = new File(dataSetA).getName();
        int pos = dataSetAFileName.lastIndexOf('.');
        if (pos > 0) {
            dataSetAFileName = dataSetAFileName.substring(0, pos);
        }
        String dataSetBFileName = new File(dataSetB).getName();
        pos = dataSetBFileName.lastIndexOf('.');
        if (pos > 0) {
            dataSetBFileName = dataSetBFileName.substring(0, pos);
        }


        String dataSetAOutFileName = outFolder + dataSetAFileName + ".diff.json";
        String dataSetBOutFileName = outFolder + dataSetBFileName + ".diff.json";
        String outFileName = outFolder + dataSetAFileName + "-" + dataSetBFileName + ".diff.json";
        long toDataSetA = 0, toDataSetB = 0, toBothDataSets = 0;

        System.out.println("Reading keys from second dataset and compare");
        try (ExtendedWriter writerDiffB = new ExtendedWriter(dataSetBOutFileName)) {
            try (ExtendedWriter writerDiffAB = new ExtendedWriter(outFileName)) {
                try (BufferedReader reader = new BufferedReader(new FileReader(dataSetB))) {
                    String line;
                    long lineNumber = 0;
                    while ((line = reader.readLine()) != null) {
                        lineNumber++;
                        if (line.trim().length() == 0) continue;
                        try {
                            ClassificationKey key = new ClassificationKey(line);
                            boolean same = false;
                            for (ClassificationKey tmpKey : keys) {
                                if (tmpKey.getRsaKey().getModulus().equals(key.getRsaKey().getModulus())) {
                                    writerDiffAB.writeln(key.mergeWith(tmpKey).toJSON().toJSONString());
                                    toBothDataSets++;
                                    keys.remove(tmpKey);
                                    same = true;
                                    break;
                                }
                            }
                            if (!same) {
                                toDataSetB++;
                                writerDiffB.writeln(key.toJSON().toJSONString());
                            }
                        }
                        catch (Exception ex) {
                            System.err.println("Cannot read key on line '" + lineNumber +"' from dataset '" + dataSetB + "': " + ex.getMessage());
                        }
                    }
                }
            }
        }
        catch (IOException ex) {
            System.err.println("Error while writing to file '" + dataSetBOutFileName + "'/'" + outFileName + "': " + ex.getMessage());
        }

        System.out.println("Writing keys from first dataset.");
        try (ExtendedWriter writerDiffA = new ExtendedWriter(dataSetAOutFileName)) {
            for (ClassificationKey key : keys) {
                writerDiffA.writeln(key.toJSON().toJSONString());
                toDataSetA++;
            }
        }
        catch (IOException ex) {
            System.err.println("Error while writing to file '" + dataSetAOutFileName + "'/'" + dataSetBOutFileName + "'/'" + outFileName + "': " + ex.getMessage());
        }

        System.out.println("Diff between two classification datasets:");
        System.out.println("In dataset A ('" + dataSetA + "'): " + toDataSetA + " keys");
        System.out.println("In dataset B ('" + dataSetB + "'): " + toDataSetB + " keys");
        System.out.println("In both dataset: " + toBothDataSets + " keys");
    }
}
