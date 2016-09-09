package cz.crcs.sekan.rsakeysanalysis.tools;

import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 20.06.2016
 */
public class CumulateResults {
    public static void run(String infolder, String outfile) {
        Map<String, Map<Long, Long>> uniqueKeysCount = new TreeMap<>();
        Map<String, Map<Long, Long>> allKeysCount = new TreeMap<>();

        Map<String, Map<Long, String>> uniqueKeysPositive = new TreeMap<>();
        Map<String, Map<Long, String>> uniqueKeysNegative = new TreeMap<>();
        Map<String, Map<Long, String>> allKeysPositive = new TreeMap<>();
        Map<String, Map<Long, String>> allKeysNegative = new TreeMap<>();

        long keys[] = {1L, 2L, 10L, 100L};
        File file = new File(infolder);
        for(String name : file.list())
        {
            String dirName = infolder + "\\" + name;
            if (new File(dirName).isDirectory())
            {
                uniqueKeysCount.put(name, new TreeMap<>());
                allKeysCount.put(name, new TreeMap<>());
                uniqueKeysPositive.put(name, new TreeMap<>());
                uniqueKeysNegative.put(name, new TreeMap<>());
                allKeysPositive.put(name, new TreeMap<>());
                allKeysNegative.put(name, new TreeMap<>());

                for (long key : keys) {
                    String infile = dirName + "\\" + "groups - common_name - sameDay - " + key + " keys for classification.csv";
                    try (BufferedReader reader = new BufferedReader(new FileReader(infile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("Keys All;")) {
                                String val = line.split(";", 2)[1];
                                allKeysCount.get(name).put(key, Long.valueOf(val));
                            }
                            else if (line.startsWith("Keys Unique;")) {
                                String val = line.split(";", 2)[1];
                                uniqueKeysCount.get(name).put(key, Long.valueOf(val));
                            }
                            else if (line.equals("Positive;Unique")) {
                                reader.readLine();
                                line = reader.readLine();
                                uniqueKeysPositive.get(name).put(key, line.split(";",2)[1]);
                            }
                            else if (line.equals("Positive")) {
                                reader.readLine();
                                line = reader.readLine();
                                allKeysPositive.get(name).put(key, line.split(";",2)[1]);
                            }
                            else if (line.equals("Negative;Unique")) {
                                reader.readLine();
                                line = reader.readLine();
                                uniqueKeysNegative.get(name).put(key, line.split(";",2)[1]);
                            }
                            else if (line.equals("Negative")) {
                                reader.readLine();
                                line = reader.readLine();
                                allKeysNegative.get(name).put(key, line.split(";",2)[1]);
                            }
                        }
                    }
                    catch (IOException ex) {
                        System.err.println("Error while reading file '" + infile + "': " + ex.getMessage());
                    }
                }
            }
        }


        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
        otherSymbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#0.00000000", otherSymbols);
        try (ExtendedWriter writer = new ExtendedWriter(outfile)) {
            writer.writeln("Classification results - only unique");
            writer.write("Minimal keys;");
            for (long key : keys) {
                writer.write(";" + key + ";;;;;;;;;;;;");
            }
            writer.newLine();
            writer.write("Groups;");
            int groups[] = {1,2,3,4,5,6,7,8,9,10,11,12,13};
            for (long key : keys) {
                for (long group : groups) {
                    writer.write(";Group " + group);
                }
            }
            writer.newLine();

            for (String source : uniqueKeysNegative.keySet()) {
                writer.write(source + ";Result");
                for (long key : keys) {
                    String unival[] = uniqueKeysPositive.get(source).get(key).split(";");
                    String negval[] = uniqueKeysNegative.get(source).get(key).split(";");
                    for (int i = 0; i < unival.length; i++) {
                        if (Long.valueOf(negval[i]).equals(uniqueKeysCount.get(source).get(key))) {
                            writer.write(";-");
                        }
                        else {
                            writer.write(";" + unival[i]);
                        }
                    }
                }
                writer.newLine();
                writer.write(";Max");
                for (long key : keys) {
                    String negval[] = uniqueKeysNegative.get(source).get(key).split(";");
                    for (int i = 0; i < negval.length; i++) {
                        if (uniqueKeysCount.get(source).get(key) == 0) {
                            writer.write(";-");
                        }
                        else {
                            writer.write(";" + formatter.format(1.0 - (Double.valueOf(negval[i])/uniqueKeysCount.get(source).get(key))));
                        }
                    }
                }
                writer.newLine();
            }
            writer.newLine();

            writer.writeln("Classification results - with duplicity");
            writer.write("Minimal keys;");
            for (long key : keys) {
                writer.write(";" + key + ";;;;;;;;;;;;");
            }
            writer.newLine();
            writer.write("Groups;");
            for (long key : keys) {
                for (long group : groups) {
                    writer.write(";Group " + group);
                }
            }
            writer.newLine();
            for (String source : uniqueKeysNegative.keySet()) {
                writer.write(source + ";Result");
                for (long key : keys) {
                    String unival[] = allKeysPositive.get(source).get(key).split(";");
                    String negval[] = allKeysNegative.get(source).get(key).split(";");
                    for (int i = 0; i < unival.length; i++) {
                        if (Long.valueOf(negval[i]).equals(allKeysCount.get(source).get(key))) {
                            writer.write(";-");
                        }
                        else {
                            writer.write(";" + unival[i]);
                        }
                    }
                }
                writer.newLine();
                writer.write(";Max");
                for (long key : keys) {
                    String negval[] = allKeysNegative.get(source).get(key).split(";");
                    for (int i = 0; i < negval.length; i++) {
                        if (allKeysCount.get(source).get(key) == 0) {
                            writer.write(";-");
                        }
                        else {
                            writer.write(";" + formatter.format(1.0 - (Double.valueOf(negval[i])/allKeysCount.get(source).get(key))));
                        }
                    }
                }
                writer.newLine();
            }
            writer.newLine();

            writer.writeln("Keys count");
            for (String source : uniqueKeysNegative.keySet()) {
                writer.write(";" + source + ";;;");
            }
            writer.newLine();
            writer.write("Minimal keys");
            for (String source : uniqueKeysNegative.keySet()) {
                for (long key : keys) {
                    writer.write(";" + key);
                }
            }

            writer.newLine();
            writer.write("Unique");
            for (String source : uniqueKeysNegative.keySet()) {
                for (long key : keys) {
                    writer.write(";" + uniqueKeysCount.get(source).get(key));
                }
            }

            writer.newLine();
            writer.write("With duplicity");
            for (String source : uniqueKeysNegative.keySet()) {
                for (long key : keys) {
                    writer.write(";" + allKeysCount.get(source).get(key));
                }
            }
            writer.newLine();
        }
        catch (IOException ex) {
            System.err.println("Error while writing to file '" + outfile + "': " + ex.getMessage());
        }
    }
}
