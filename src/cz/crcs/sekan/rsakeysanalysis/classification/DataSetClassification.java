package cz.crcs.sekan.rsakeysanalysis.classification;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.PrimeMatchingContainer;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.ModulusFactors;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import cz.crcs.sekan.rsakeysanalysis.template.Template;
import javafx.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

    public void classify(boolean batchBySharedPrimes) {
        Map<Set<String>, ClassificationContainer> parsedData = new HashMap<>();
        List<ClassificationContainer> keysWithoutSource = new ArrayList<>();
        // Private keys obtained by factorization should share some primes.
        // By mapping the primes to the keys, we can eventually create a group of keys probably coming from the same
        // flawed implementation without relying on meta-data for grouping.
        Map<BigInteger, Set<PrimeMatchingContainer>> primeToKeyMap = new HashMap<>();

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

                    // Allow batching by primes if shared primes are available
                    if (batchBySharedPrimes && key.getRsaKey().getP() != null && key.getRsaKey().getQ() != null) {
                        BigInteger[] array = {key.getRsaKey().getP(), key.getRsaKey().getQ()};
                        for (BigInteger prime : array) {
                            Set<PrimeMatchingContainer> keySet = primeToKeyMap.get(prime);

                            PrimeMatchingContainer container = new PrimeMatchingContainer(
                                            key.getCount(),
                                            row,
                                            key,
                                            key.getRsaKey().getP(),
                                            key.getRsaKey().getQ());
                            if (keySet == null) {
                                HashSet<PrimeMatchingContainer> newSet = new HashSet<>();
                                newSet.add(container);
                                primeToKeyMap.put(prime, newSet);
                            } else {
                                keySet.add(container);
                            }
                        }
                    }
                    //Create new batch by source identification if does not exist
                    else if (key.getSource() == null || key.getSource().size() == 0) {
                        keysWithoutSource.add(new ClassificationContainer(key.getCount(), row, key));
                    }
                    else {
                        ClassificationContainer container = parsedData.get(key.getSource());
                        if (container == null) {
                            parsedData.put(key.getSource(), new ClassificationContainer(key.getCount(), row, key));
                        } else {
                            //If batch exists add classified row to container
                            container.add(key.getCount(), row, key);
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

        if (primeToKeyMap != null && !primeToKeyMap.isEmpty()) {
            // Keys were batched by primes. Merge and remove sets and do the actual classification
            // Each prime is a key in the map to a set of ClassificationContainers
            Map<Set<BigInteger>, Set<PrimeMatchingContainer>> uniqueSets = new HashMap<>();
            while (!primeToKeyMap.isEmpty()) {
                BigInteger firstPrime = primeToKeyMap.keySet().iterator().next();
                Set<PrimeMatchingContainer> allKeys = primeToKeyMap.get(firstPrime);
                Set<PrimeMatchingContainer> processedKeys = new HashSet<>();

                Set<BigInteger> sharedPrimes = new HashSet<>();

                while (!allKeys.isEmpty()) {
                    PrimeMatchingContainer singleKey = allKeys.iterator().next();
                    for (BigInteger prime : new BigInteger[] {singleKey.getP(), singleKey.getQ()}) {
                        Set<PrimeMatchingContainer> similarKeys = primeToKeyMap.get(prime);
                        if (similarKeys != null) {
                            allKeys.addAll(similarKeys);
                            primeToKeyMap.remove(prime);
                            sharedPrimes.add(prime);
                        }
                    }
                    allKeys.remove(singleKey);
                    processedKeys.add(singleKey);
                }
                primeToKeyMap.remove(firstPrime);
                uniqueSets.put(sharedPrimes, processedKeys);
            }

            for (Set<BigInteger> mapKey : uniqueSets.keySet()) {
                Set<String> stringKey = new HashSet<>();
                for (BigInteger i : mapKey) {
                    stringKey.add(i.toString(16));
                }
                ClassificationContainer mainContainer = null;
                for (ClassificationContainer container : uniqueSets.get(mapKey)) {
                    if (mainContainer == null) {
                        mainContainer = container;
                    } else {
                        mainContainer.add(container.getNumOfKeys(), container.getRow(), container.getKeys());
                    }
                }
                mainContainer.getRow().normalize();
                parsedData.put(stringKey, mainContainer);
            }
        }

        ExtendedWriter datasetWriter;

        try {
            datasetWriter = new ExtendedWriter(pathToFolderWithResults + "dataset.json");
        } catch (IOException ex) {
            System.err.println("Error while opening file 'dataset.json' for results.");
            return;
        }

        try {
            datasetWriter.write("{"); // start of meta information object
            datasetWriter.write("\"groups\":"); // start of array for groups

            JSONArray groupsArray = new JSONArray();
            List<JSONObject> groups = new LinkedList<>();
            int groupIndex = 0;
            for (String groupName : table.getGroupsNames()) {
                JSONObject group = new JSONObject();
                group.put("index", groupIndex++);
                group.put("name", groupName);
                JSONArray sources = new JSONArray();
                sources.addAll(table.getGroupSources(groupName));
                group.put("sources", sources);
                groups.add(group);
            }
            groupsArray.addAll(groups);
            groupsArray.writeJSONString(datasetWriter);

            if (table.getRawTable() != null) {
                datasetWriter.write(", \"table\":");
                table.getRawTable().toJSONObject().writeJSONString(datasetWriter);
            }
            datasetWriter.writeln("}"); // end of array for groups

        } catch (IOException e) {
            System.err.println("Error while writing classification info to file 'dataset.json'.");
        }

        //Reconstruct original dataset with results of each classification container
        Template fullClassificationContainerResult;
        try {
            fullClassificationContainerResult = new Template("fullClassificationContainerResult.csv");
        }
        catch (IOException ex) {
            System.err.println("Error while creating template from file 'fullClassificationContainerResult.csv': " + ex.getMessage());
            return;
        }

        long batchId = 0;

        for (Set<String> batchKey : parsedData.keySet()) {
            ClassificationContainer container = parsedData.get(batchKey);
            ClassificationRow row = container.getRow();
            List<ClassificationKey> keys = container.getKeys();

            for (ClassificationKey key : keys) {
                try {
                    String values = "";
                    for (String groupName : table.getGroupsNames()) {
                        BigDecimal val = row.getSource(groupName);
                        values += (values.length() > 0 ? "," : "");
                        if (val != null) { values += val.doubleValue(); } else { values += "0"; }
                    }

                    JSONArray vectorsArray = new JSONArray();
                    vectorsArray.addAll(table.generationIdentification(key));
                    String vectors = vectorsArray.toJSONString();

                    JSONArray sourceArray = new JSONArray();
                    if (key.getSource() != null) sourceArray.addAll(key.getSource());
                    String source = sourceArray.toJSONString();

                    fullClassificationContainerResult.resetVariables();
                    fullClassificationContainerResult.setVariable("p", key.getRsaKey().getP().toString(16));
                    fullClassificationContainerResult.setVariable("q", key.getRsaKey().getQ().toString(16));
                    fullClassificationContainerResult.setVariable("n", key.getRsaKey().getModulus().toString(16));
                    fullClassificationContainerResult.setVariable("ordered", Boolean.valueOf(key.isOrdered()).toString());
                    fullClassificationContainerResult.setVariable("occurrence", Long.valueOf(key.getCount()).toString());
                    fullClassificationContainerResult.setVariable("source", source);
                    fullClassificationContainerResult.setVariable("info", key.getInfo().toString());
                    fullClassificationContainerResult.setVariable("batch", Long.valueOf(batchId).toString());
                    fullClassificationContainerResult.setVariable("vector", vectors);
                    fullClassificationContainerResult.setVariable("probabilities", values);
                    datasetWriter.write(fullClassificationContainerResult.generateString());
                } catch (IOException ex) {
                    System.err.println("Error while writing dataset to file.");
                }
            }

            batchId++;
        }

        try {
            datasetWriter.close();
        } catch (IOException e) {
            System.err.println("Error while closing dataset.json");
            return;
        }

        try {
            datasetWriter = new ExtendedWriter(pathToFolderWithResults + "dataset.csv");
            datasetWriter.write("subject_dn,valid_start,valid_end,modulus,p,q,suspected_vendor,key_id,batch_id,batch_size,duplicity_id,duplicity_count,classification");
            for (String groupName : table.getGroupsNames()) {
                datasetWriter.write(",");
                datasetWriter.write(groupName);
            }
            datasetWriter.newLine();
        } catch (IOException ex) {
            System.err.println("Error while opening file 'dataset.csv' for results.");
            return;
        }

        //Reconstruct original dataset with results of each key in each classification container
        Template completeClassificationContainerResult;
        try {
            completeClassificationContainerResult = new Template("completeClassificationContainerResult.csv");
        }
        catch (IOException ex) {
            System.err.println("Error while creating template from file 'completeClassificationContainerResult.csv': " + ex.getMessage());
            return;
        }

        batchId = 0;
        long uniqueKeyId = 0;
        boolean pqDataset = true;

        for (Set<String> batchKey : parsedData.keySet()) {
            if (!pqDataset) {break;}
            ClassificationContainer container = parsedData.get(batchKey);
            ClassificationRow row = container.getRow();
            List<ClassificationKey> keys = container.getKeys();

            int batchSize = keys.size();
            for (ClassificationKey key : keys) {
                try {
                    Set<String> attributeNames = key.getInfo().keySet();
                    if (!attributeNames.contains("subject_dn") || !attributeNames.contains("valid_start")
                            || !attributeNames.contains("valid_end") || !attributeNames.contains("suspected_vendor")) {
                        // this is not PQ dataset
                        System.out.println("Info: Not a PQ dataset");
                        pqDataset = false;
                        break;
                    }

                    List<String> subjects = extractAttributes("subject_dn", key.getInfo());
                    List<String> validStarts = extractAttributes("valid_start", key.getInfo());
                    List<String> validEnds = extractAttributes("valid_end", key.getInfo());
                    List<String> suspectedVendors = extractAttributes("suspected_vendor", key.getInfo());

                    int actualKeyCount = Arrays.asList(subjects.size(), validStarts.size(), validEnds.size(), suspectedVendors.size())
                            .stream().min((o1, o2) -> o1.compareTo(o2) < 0 ? o1 : o2).orElseGet(() -> 0);

                    if (key.getCount() != actualKeyCount) {
                        System.err.println("Warning: the number of attribute values does not match the number of keys");
                    }

                    Set<String> vectorList = table.generationIdentification(key);
                    String vectors = "";
                    for (Iterator<String> iterator = vectorList.iterator(); iterator.hasNext(); ) {
                        vectors = vectors.concat(iterator.next());
                        if (iterator.hasNext()) vectors = vectors.concat(",");
                    }


                    String values = "";
                    for (String groupName : table.getGroupsNames()) {
                        BigDecimal val = row.getSource(groupName);
                        values += (values.length() > 0 ? "," : "");
                        if (val != null) { values += val.doubleValue(); } else { values += "0"; }
                    }

                    for (int i = 0; i < actualKeyCount; i++) {
                        completeClassificationContainerResult.resetVariables();
                        completeClassificationContainerResult.setVariable("p", key.getRsaKey().getP().toString(16));
                        completeClassificationContainerResult.setVariable("q", key.getRsaKey().getQ().toString(16));
                        completeClassificationContainerResult.setVariable("modulus", key.getRsaKey().getModulus().toString(16));
                        completeClassificationContainerResult.setVariable("subject_dn", subjects.get(i).replace(",", ";"));
                        completeClassificationContainerResult.setVariable("valid_start", validStarts.get(i));
                        completeClassificationContainerResult.setVariable("valid_end", validEnds.get(i));
                        completeClassificationContainerResult.setVariable("suspected_vendor", suspectedVendors.get(i));
                        completeClassificationContainerResult.setVariable("unique_id", Long.valueOf(uniqueKeyId).toString());
                        completeClassificationContainerResult.setVariable("batch_id", Long.valueOf(batchId).toString());
                        completeClassificationContainerResult.setVariable("batch_size", Long.valueOf(batchSize).toString());
                        completeClassificationContainerResult.setVariable("duplicity_id", Long.valueOf(i).toString());
                        completeClassificationContainerResult.setVariable("duplicity_count", Long.valueOf(actualKeyCount).toString());
                        completeClassificationContainerResult.setVariable("vector", vectors);
                        completeClassificationContainerResult.setVariable("probabilities", values);
                        datasetWriter.write(completeClassificationContainerResult.generateString());
                        uniqueKeyId++;
                    }
                } catch (IOException ex) {
                    System.err.println("Error while writing dataset to file.");
                }
            }

            batchId++;
        }

        try {
            datasetWriter.close();
        } catch (IOException e) {
            System.err.println("Error while closing dataset.csv");
            return;
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

    private static List<String> extractAttributes(String attributeName, JSONObject jsonObject) {
        Object possibleAttributes = jsonObject.get(attributeName);
        List<String> attributes = new LinkedList<>();
        if (possibleAttributes == null) {
            System.err.println("Warning: null " + attributeName);
        } else if (possibleAttributes instanceof String) {
            attributes.add((String) possibleAttributes);
        } else if (possibleAttributes instanceof Collection) {
            attributes.addAll((Collection<String>) possibleAttributes);
        } else {
            System.err.println("Warning: " + attributeName + " of invalid type: " + possibleAttributes.getClass());
        }
        return attributes;
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
