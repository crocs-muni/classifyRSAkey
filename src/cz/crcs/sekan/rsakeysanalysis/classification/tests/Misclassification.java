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
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 12.06.2016
 */
public class Misclassification {
    private Random generator;

    private final int keysForTest[] = {1, 5, 10, 100};

    private ClassificationTable classificationTable;
    private RawTable testsKeysTable;

    public Misclassification(String infile, long keys)  throws IOException, ParseException, NoSuchAlgorithmException, WrongTransformationFormatException, TransformationNotFoundException {
        RawTable table = RawTable.load(infile);
        Pair<RawTable, RawTable> pair = table.splitForTests(keys);
        RawTable withoutTestsKeysTable = pair.getValue();
        classificationTable = withoutTestsKeysTable.computeClassificationTable();
        testsKeysTable = pair.getKey();
        generator = SecureRandom.getInstance("SHA1PRNG");
    }

    public void compute(String outfile) throws IOException {
        try (ExtendedWriter writer = new ExtendedWriter(outfile)) {
            for (int keyForTest : keysForTest) {
                writer.writeln("Misclassification;" + keyForTest + " keys");
                writeClassificationResultToCsv(computeAllForNKeys(keyForTest, testsKeysTable.copyTable()), writer);
                writer.newLine();
            }

            for (int keyForTest : keysForTest) {
                writer.writeln("Misclassification;" + keyForTest + " keys;Top 1");
                writeResultToCsv(computeTopForNKeys(keyForTest, testsKeysTable.copyTable()), writer);
                writer.newLine();
            }
        }
    }

    private Map<String, ClassificationRow> computeAllForNKeys(int keys, Map<String, Map<String, Long>> testKeys) {
        Map<String, ClassificationRow> result = new TreeMap<>();

        for (String group : classificationTable.getGroupsNames()) {
            ClassificationRow resultRow = null;
            for (String source : classificationTable.getGroupSources(group)) {
                ClassificationContainer container = null;
                while (testKeys.get(source).keySet().size() > 0) {
                    String randomIdentification = getRandomIdentification(testKeys.get(source));
                    ClassificationRow row = classificationTable.classifyIdentification(randomIdentification);

                    if (row == null) {
                        System.out.println("Cannot find classificationRow with identification '" + randomIdentification + "'");
                        continue;
                    }

                    if (container == null) container = new ClassificationContainer(1, row);
                    else container.add(1, row);

                    if (container.getNumOfRows() == keys) {
                        if (resultRow == null) resultRow = row;
                        else resultRow = resultRow.computeWithNotSameSource(row);
                        container = null;
                    }
                }
            }
            result.put(group, resultRow);
        }
        return result;
    }

    private Map<String, Map<String, Double>> computeTopForNKeys(int keys, Map<String, Map<String, Long>> testKeys) {
        Map<String, Map<String, Double>> result = new TreeMap<>();

        for (String group : classificationTable.getGroupsNames()) {
            Map<String, Long> groupResult = new TreeMap<>();
            for (String source : classificationTable.getGroupSources(group)) {
                ClassificationContainer container = null;
                while (testKeys.get(source).keySet().size() > 0) {
                    String randomIdentification = getRandomIdentification(testKeys.get(source));
                    ClassificationRow row = classificationTable.classifyIdentification(randomIdentification);

                    if (row == null) {
                        System.out.println("Cannot find classificationRow with identification '" + randomIdentification + "'");
                        continue;
                    }

                    if (container == null) container = new ClassificationContainer(1, row);
                    else container.add(1, row);

                    if (container.getNumOfRows() == keys) {
                        String top = container.getRow().getTopGroups(1).iterator().next();
                        groupResult.putIfAbsent(top, 0L);
                        groupResult.put(top, groupResult.get(top) + 1);
                        container = null;
                    }
                }
            }
            long sum = groupResult.values().stream().mapToLong(Long::longValue).sum();
            Map<String, Double> groupResultDouble = new TreeMap<>();
            for (String groupTo : groupResult.keySet()) {
                groupResultDouble.put(groupTo, groupResult.get(groupTo)/(double)sum);
            }
            result.put(group, groupResultDouble);
        }
        return result;
    }

    private String getRandomIdentification(Map<String, Long> identificationsMap) {
        Object[] identifications = identificationsMap.keySet().toArray();
        String randomIdentification = (String) identifications[generator.nextInt(identifications.length)];
        Long keysInIdentification = identificationsMap.get(randomIdentification);
        if (keysInIdentification == 1) {
            identificationsMap.remove(randomIdentification);
        } else {
            identificationsMap.put(randomIdentification, keysInIdentification - 1);
        }
        return randomIdentification;
    }

    private void writeClassificationResultToCsv(Map<String, ClassificationRow> result, ExtendedWriter writer) throws IOException {
        Map<String, Map<String, Double>> results = new TreeMap<>();
        for (String group : result.keySet()) {
            Map<String, Double> groupResult = new TreeMap<>();
            for (Map.Entry<String, BigDecimal> entry : result.get(group).getValues().entrySet()) {
                groupResult.put(entry.getKey(), entry.getValue().doubleValue());
            }
            results.put(group, groupResult);
        }
        writeResultToCsv(results, writer);
    }

    private void writeResultToCsv(Map<String, Map<String, Double>> result, ExtendedWriter writer) throws IOException {
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
        otherSymbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#0.0000", otherSymbols);
        writer.writeln(";" + String.join(";", result.keySet()));
        for (String group : result.keySet()) {
            writer.write(group);
            for (String groupTo : result.keySet()) {
                Double decimal = result.get(group).get(groupTo);
                if (decimal == null) {
                    writer.write(";-");
                }
                else {
                    double value = decimal;
                    writer.write(";" + formatter.format(value));
                }
            }
            writer.newLine();
        }
    }

}
