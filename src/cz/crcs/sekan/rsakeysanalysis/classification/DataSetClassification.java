package cz.crcs.sekan.rsakeysanalysis.classification;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.ModulusFactors;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import cz.crcs.sekan.rsakeysanalysis.template.Template;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 03/10/2016
 */
public class DataSetClassification {
    private ClassificationTable table;
    private String pathToDataSet;
    private String pathToFolderWithResults;
    private Long minKeys[] = {1L, 2L, 10L, 100L};

    /**
     * Static class for identification which factors check on modulus (null = do not check factors).
     */
    private ModulusFactors modulusFactors = null;

    /**
     * Classification table for identification which keys should not be classified.
     */
    private ClassificationTable classificationTableForNotClassify = null;

    /**
     * Decimal number formatter
     */
    private DecimalFormat formatter;

    public DataSetClassification(ClassificationTable table, String pathToDataSet, String pathToFolderWithResults) {
        if (table == null) throw new IllegalArgumentException("Table in DataSetClassification constructor cannot be null.");
        if (pathToDataSet == null) throw new IllegalArgumentException("Path to dataset in DataSetClassification constructor cannot be null.");
        if (pathToFolderWithResults == null) throw new IllegalArgumentException("Path to folder with results in DataSetClassification constructor cannot be null.");

        //Create folder for results if does not exist
        File folderFile = new File(pathToFolderWithResults);
        if (!folderFile.exists()) {
            if (!folderFile.mkdirs()) {
                throw new IllegalArgumentException("Cannot create folder.");
            }
        }

        //Check if path to folder is correctly ended
        if (!pathToFolderWithResults.endsWith("/") && !pathToFolderWithResults.endsWith("\\")) {
            pathToFolderWithResults += "/";
        }

        this.table = table;
        this.pathToDataSet = pathToDataSet;
        this.pathToFolderWithResults = pathToFolderWithResults;

        DecimalFormatSymbols decimalFormatter = new DecimalFormatSymbols();
        decimalFormatter.setDecimalSeparator('.');
        this.formatter = new DecimalFormat("#0.00000000", decimalFormatter);
    }

    public void setModulusFactors(ModulusFactors modulusFactors) {
        this.modulusFactors = modulusFactors;
    }

    public void setClassificationTableForNotClassify(ClassificationTable classificationTableForNotClassify) {
        this.classificationTableForNotClassify = classificationTableForNotClassify;
    }

    public void classify() {
        Map<Set<String>, ClassificationContainer> parsedData = new HashMap<>();
        List<ClassificationContainer> keysWithoutSource = new ArrayList<>();

        //Read and parse all keys
        try (BufferedReader reader = new BufferedReader(new FileReader(pathToDataSet))) {
            String jsonLine;
            long lineNumber = 0;
            while ((jsonLine = reader.readLine()) != null) {
                lineNumber++;
                //Count the lines and each 100000 print actual line
                if (lineNumber % 100000 == 0) System.out.println("Parsed " + lineNumber + " lines.");

                //Skip blank lines
                if (jsonLine.length() == 0) continue;

                try {
                    ClassificationKey key = new ClassificationKey(jsonLine);
                    //Check for modulus factors (p or q)
                    if (modulusFactors != null) {
                        BigInteger factor = modulusFactors.getFactor(key.getRsaKey());
                        if (factor != null) {
                            System.out.println("FACTOR: Line: " + lineNumber + ", Factor: 0x" + factor.toString(16) + ", RSAKey: " + key.getRsaKey());
                        }
                    }

                    //Check if key would not be classify
                    if (classificationTableForNotClassify != null) {
                        if (classificationTableForNotClassify.classifyKey(key) == null) {
                            System.out.println("NC: '" + table.generationIdentification(key).toString() + "', Key: " + key);
                            continue;
                        }
                    }

                    //Classify key
                    ClassificationRow row = table.classifyKey(key);
                    if (row == null) {
                        System.err.println("Classification table does not contain identification '" + table.generationIdentification(key).toString() + "', Key: " + key);
                        continue;
                    }

                    //Create new batch by source identification if does not exist
                    if (key.getSource() == null || key.getSource().size() == 0) {
                        keysWithoutSource.add(new ClassificationContainer(key.getCount(), row));
                    }
                    else {
                        ClassificationContainer container = parsedData.get(key.getSource());
                        if (container == null) {
                            parsedData.put(key.getSource(), new ClassificationContainer(key.getCount(), row));
                        } else {
                            //If batch exists add classified row to container
                            container.add(key.getCount(), row);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error: cannot parse line " + lineNumber + ": " + ex.getMessage());
                    System.err.println("Json line: " + jsonLine);
                }
            }
        } catch (IOException ex) {
            System.err.println("Error while reading file '" + pathToDataSet + "'.");
        }

        // Create containers for statistics
        Map<Long, ExtendedWriter> outputsAll = new HashMap<>();
        Map<Long, Long> groupsCount = new HashMap<>();
        Map<Long, Long> groupsCountUnique = new HashMap<>();
        Map<Long, Map<String, BigDecimal>> groupsPositiveCountAll = new HashMap<>();
        Map<Long, Map<String, BigDecimal>> groupsPositiveCountUniqueAll = new HashMap<>();
        Map<Long, Map<String, Long>> groupsNegativeCountAll = new HashMap<>();
        Map<Long, Map<String, Long>> groupsNegativeCountUniqueAll = new HashMap<>();
        List<Pair<Long, Long>> minMaxKeys = new ArrayList<>();

        //Initialize containers
        for (int i = 0; i < minKeys.length; i++) {
            Long minKey = minKeys[i], maxKey = null;
            if (i < minKeys.length - 1) maxKey = minKeys[i + 1];
            minMaxKeys.add(new Pair<>(minKey, maxKey));

            groupsCount.put(minKey, 0L);
            groupsCountUnique.put(minKey, 0L);

            String file = "containers results, " + minKey + (maxKey != null ? " - " + String.valueOf(maxKey-1) : " and more") + " keys.csv";
            try {
                ExtendedWriter writer = new ExtendedWriter(pathToFolderWithResults + file);
                outputsAll.put(minKey, writer);
            } catch (IOException ex) {
                System.err.println("Error while opening file '" + file + "' for results.");
                return;
            }

            Map<String, BigDecimal> groupsPositiveCount = new TreeMap<>();
            Map<String, BigDecimal> groupsPositiveCountUnique = new TreeMap<>();
            Map<String, Long> groupsNegativeCount = new TreeMap<>();
            Map<String, Long> groupsNegativeCountUnique = new TreeMap<>();
            for (String groupName : table.getGroupsNames()) {
                groupsPositiveCount.put(groupName, BigDecimal.ZERO);
                groupsPositiveCountUnique.put(groupName, BigDecimal.ZERO);
                groupsNegativeCount.put(groupName, 0L);
                groupsNegativeCountUnique.put(groupName, 0L);
            }
            groupsPositiveCountAll.put(minKey, groupsPositiveCount);
            groupsPositiveCountUniqueAll.put(minKey, groupsPositiveCountUnique);
            groupsNegativeCountAll.put(minKey, groupsNegativeCount);
            groupsNegativeCountUniqueAll.put(minKey, groupsNegativeCountUnique);
        }

        //Merge all results
        List<Pair<Set<String>, ClassificationContainer>> entries = new ArrayList<>();
        for (Map.Entry<Set<String>,ClassificationContainer> entry : parsedData.entrySet()) {
            entries.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        for (ClassificationContainer entry : keysWithoutSource) {
            entries.add(new Pair<>(null, entry));
        }

        //Compute statistics and save results of each classification container
        Template classificationContainerResult;
        try {
            classificationContainerResult = new Template("classificationContainerResult.csv");
        }
        catch (IOException ex) {
            System.err.println("Error while creating template from file 'classificationContainerResult.csv': " + ex.getMessage());
            return;
        }
        for (Pair<Set<String>,ClassificationContainer> entry : entries) {
            ClassificationRow row = entry.getValue().getRow();
            row.normalize();
            long allKeys = entry.getValue().getNumOfKeys();
            long uniqueKeys = entry.getValue().getNumOfRows();

            for (Pair<Long, Long> minMaxKey : minMaxKeys) {
                Long minKey = minMaxKey.getKey(), maxKey = minMaxKey.getValue();

                ExtendedWriter writer = outputsAll.get(minKey);
                if (entry.getValue().getNumOfRows() < minKey || writer == null) continue;
                if (maxKey != null) {
                    if (entry.getValue().getNumOfRows() >= maxKey) continue;
                }

                groupsCount.put(minKey, groupsCount.get(minKey) + entry.getValue().getNumOfKeys());
                groupsCountUnique.put(minKey, groupsCountUnique.get(minKey) + entry.getValue().getNumOfRows());

                Map<String, BigDecimal> groupsPositiveCount = groupsPositiveCountAll.get(minKey);
                Map<String, BigDecimal> groupsPositiveCountUnique = groupsPositiveCountUniqueAll.get(minKey);
                Map<String, Long> groupsNegativeCount = groupsNegativeCountAll.get(minKey);
                Map<String, Long> groupsNegativeCountUnique = groupsNegativeCountUniqueAll.get(minKey);

                try {
                    String groups = String.join(",", table.getGroupsNames()), values = "";
                    for (String groupName : table.getGroupsNames()) {
                        BigDecimal val = row.getSource(groupName);
                        if (val != null) {
                            values += (values.length() > 0 ? "," : "") + formatter.format(val.doubleValue());
                            groupsPositiveCount.put(groupName, groupsPositiveCount.get(groupName).add(val.multiply(BigDecimal.valueOf(allKeys))));
                            groupsPositiveCountUnique.put(groupName, groupsPositiveCountUnique.get(groupName).add(val.multiply(BigDecimal.valueOf(uniqueKeys))));
                        }
                        else {
                            values += (values.length() > 0 ? "," : "") + "-";
                            groupsNegativeCount.put(groupName, groupsNegativeCount.get(groupName) + allKeys);
                            groupsNegativeCountUnique.put(groupName, groupsNegativeCountUnique.get(groupName) + uniqueKeys);
                        }
                    }

                    classificationContainerResult.resetVariables();
                    classificationContainerResult.setVariable("source", (entry.getKey() != null ? entry.getKey().toString() : "null"));
                    classificationContainerResult.setVariable("uniqueKeys", String.valueOf(uniqueKeys));
                    classificationContainerResult.setVariable("keys", String.valueOf(allKeys));
                    classificationContainerResult.setVariable("groups", groups);
                    classificationContainerResult.setVariable("values", values);
                    writer.write(classificationContainerResult.generateString());
                } catch (IOException ex) {
                    System.err.println("Error while writing result to file.");
                }
            }
        }

        //Save results of classification
        Template classificationResult;
        try {
            classificationResult = new Template("classificationResult.csv");
        }
        catch (IOException ex) {
            System.err.println("Error while creating template from file 'classificationResult.csv': " + ex.getMessage());
            return;
        }
        for (Pair<Long, Long> minMaxKey : minMaxKeys) {
            Long minKey = minMaxKey.getKey(), maxKey = minMaxKey.getValue();
            try {
                ExtendedWriter writer = outputsAll.get(minKey);
                if (writer != null) writer.close();
            } catch (IOException ex) {
                System.err.println("Error while closing file for results.");
            }

            String file = "results, " + minKey + (maxKey != null ? " - " + String.valueOf(maxKey-1) : " and more") + " keys.csv";
            try (ExtendedWriter writer = new ExtendedWriter(pathToFolderWithResults + file)) {
                Pair<String, String> positiveResults = positiveVectorToCsv(groupsPositiveCountAll.get(minKey), groupsCount.get(minKey));
                Pair<String, String> positiveUniqueResults = positiveVectorToCsv(groupsPositiveCountUniqueAll.get(minKey), groupsCountUnique.get(minKey));
                Pair<String, String> negativeResults = negativeVectorToCsv(groupsNegativeCountAll.get(minKey));
                Pair<String, String> negativeUniqueResults = negativeVectorToCsv(groupsNegativeCountUniqueAll.get(minKey));

                classificationResult.resetVariables();
                classificationResult.setVariable("keys", groupsCount.get(minKey).toString());
                classificationResult.setVariable("uniqueKeys", groupsCountUnique.get(minKey).toString());

                classificationResult.setVariable("positiveGroups", positiveResults.getKey());
                classificationResult.setVariable("positiveValues", positiveResults.getValue());
                classificationResult.setVariable("positiveUniqueGroups", positiveUniqueResults.getKey());
                classificationResult.setVariable("positiveUniqueValues", positiveUniqueResults.getValue());
                classificationResult.setVariable("negativeGroups", negativeResults.getKey());
                classificationResult.setVariable("negativeValues", negativeResults.getValue());
                classificationResult.setVariable("negativeUniqueGroups", negativeUniqueResults.getKey());
                classificationResult.setVariable("negativeUniqueValues", negativeUniqueResults.getValue());
                writer.write(classificationResult.generateString());
            } catch (IOException ex) {
                System.err.println("Error while writing result to file '" + file + ".csv'.");
            }
        }
    }

    protected Pair<String, String> positiveVectorToCsv(Map<String, BigDecimal> vector, Long count) {
        String groups = "", values = "";
        for (Map.Entry<String, BigDecimal> entry : vector.entrySet()) {
            groups += "," + entry.getKey();
            if (count == 0L) values += ",-";
            else values += "," + formatter.format(entry.getValue().divide(BigDecimal.valueOf(count), BigDecimal.ROUND_CEILING).doubleValue());
        }
        return new Pair<>(groups, values);
    }

    protected Pair<String, String> negativeVectorToCsv(Map<String, Long> vector) {
        String groups = "", values = "";
        for (Map.Entry<String, Long> entry : vector.entrySet()) {
            groups += "," + entry.getKey();
            values += "," + entry.getValue();
        }
        return new Pair<>(groups, values);
    }
}
