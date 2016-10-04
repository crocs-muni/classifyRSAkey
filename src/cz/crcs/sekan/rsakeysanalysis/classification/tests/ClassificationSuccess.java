package cz.crcs.sekan.rsakeysanalysis.classification.tests;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.RawTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.TransformationNotFoundException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongTransformationFormatException;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import javafx.util.Pair;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 12.06.2016
 */
public class ClassificationSuccess {
    public static void compute(String infile, String outfile, long keys) throws IOException, ParseException, NoSuchAlgorithmException, WrongTransformationFormatException, TransformationNotFoundException {
        RawTable table = RawTable.load(infile);
        Pair<RawTable, RawTable> pair = table.splitForTests(keys);
        RawTable testsKeysTable = pair.getKey();
        RawTable withoutTestsKeysTable = pair.getValue();
        ClassificationTable classificationTable = withoutTestsKeysTable.computeClassificationTable();

        int keysForTest[] = {1, 2, 3, 4, 5, 10, 100};
        Random generator = new Random();

        //source -> keysForTest -> position
        Map<String, Map<Integer, ArrayList<Integer>>> sourcesPositions = new TreeMap<>();
        Map<String, Map<Integer, ClassificationRow>> sourcesRows = new TreeMap<>();
        for (String group : classificationTable.getGroupsNames()) {
            Map<Integer, ArrayList<Integer>> kmap = new TreeMap<>();
            Map<Integer, ClassificationRow> rmap = new TreeMap<>();
            for (int k : keysForTest) {
                //Copy table
                Map<String, Map<String, Long>> tableTemp = testsKeysTable.copyTable();
                ArrayList<Integer> positions = new ArrayList<>();
                ClassificationRow rrow = null;
                for (String source : classificationTable.getGroupSources(group)) {
                    ClassificationContainer container = null;
                    while (tableTemp.get(source).keySet().size() > 0) {
                        Object[] identifications = tableTemp.get(source).keySet().toArray();
                        String randomIdentification = (String) identifications[generator.nextInt(identifications.length)];
                        Long keysInIdentification = tableTemp.get(source).get(randomIdentification);
                        if (keysInIdentification == 1) {
                            tableTemp.get(source).remove(randomIdentification);
                        } else {
                            tableTemp.get(source).put(randomIdentification, keysInIdentification - 1);
                        }

                        ClassificationRow row = classificationTable.classifyIdentification(randomIdentification);
                        if (row == null) {
                            System.out.println("Cannot find classificationRow with identification '" + randomIdentification + "'");
                        } else {
                            if (container == null) {
                                container = new ClassificationContainer(1, row);
                            } else {
                                container.add(1, row);
                            }

                            if (container.getNumOfRows() == k) {
                                positions.add(container.getRow().getGroupPosition(group));
                                if (rrow == null) {
                                    rrow = row;
                                }
                                else {
                                    rrow = rrow.computeWithNotSameSource(row);
                                }
                                container = null;
                            }
                        }
                    }
                }

                kmap.put(k, positions);
                rmap.put(k, rrow);
            }
            sourcesPositions.put(group, kmap);
            sourcesRows.put(group, rmap);
        }

        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
        otherSymbols.setDecimalSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#00.0000", otherSymbols);
        try (ExtendedWriter writer = new ExtendedWriter(outfile)) {
            char[] charsTop = new char[keysForTest.length - 1];
            Arrays.fill(charsTop, ',');
            String separatorsTop = new String(charsTop);
            String topLine = "", bottomLine = "";
            for (int i = 0; i < 3; i++) {
                topLine += ",Top " + (i + 1) + " match" + separatorsTop;
                for (int key : keysForTest) {
                    bottomLine += "," + key;
                }
            }
            writer.writeln(topLine);
            writer.writeln(bottomLine);
            for (Map.Entry<String, Map<Integer, ArrayList<Integer>>> entrySources : sourcesPositions.entrySet()) {
                writer.write(entrySources.getKey());
                for (int k : keysForTest) {
                    ArrayList<Integer> positions = entrySources.getValue().get(k);
                    long good = positions.stream().filter(p -> p <= 1).count();
                    writer.write("," + formatter.format(((double)good)/positions.size()));
                }
                for (int k : keysForTest) {
                    ArrayList<Integer> positions = entrySources.getValue().get(k);
                    long good = positions.stream().filter(p -> p <= 2).count();
                    writer.write("," + formatter.format(((double)good)/positions.size()));
                }
                for (int k : keysForTest) {
                    ArrayList<Integer> positions = entrySources.getValue().get(k);
                    long good = positions.stream().filter(p -> p <= 3).count();
                    writer.write("," + formatter.format(((double)good)/positions.size()));
                }
                writer.newLine();
            }


//            int n = classificationTable.getGroupsNames().size();
//            char[] chars = new char[n];
//            Arrays.fill(chars, ',');
//            String separators = new String(chars);
//
//            writer.newLine();
//            for (int key : keysForTest) {
//                writer.write(key + separators);
//            }
//            writer.newLine();
//            for (int key : keysForTest) {
//                writer.write("," + String.join(",", classificationTable.getGroupsNames()));
//            }
//            writer.newLine();
//            for (Map.Entry<String, Map<Integer, ClassificationRow>> entrySources : sourcesRows.entrySet()) {
//                writer.write(entrySources.getKey());
//                for (int k : keysForTest) {
//                    ClassificationRow row = entrySources.getValue().get(k);
//                    for (String group : classificationTable.getGroupsNames()) {
//                        BigDecimal val = row.getSource(group);
//                        if (val == null) writer.write(",-");
//                        else writer.write("," + formatter.format(val.doubleValue()));
//                    }
//                }
//                writer.newLine();
//            }
        }
    }
}
