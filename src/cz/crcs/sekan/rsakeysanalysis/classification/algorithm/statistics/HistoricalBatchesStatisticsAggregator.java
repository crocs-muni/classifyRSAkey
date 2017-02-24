package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import cz.crcs.sekan.rsakeysanalysis.template.Template;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * @author xnemec1
 * @version 11/24/16.
 */
public class HistoricalBatchesStatisticsAggregator implements StatisticsAggregator {
    private Map<Long, ExtendedWriter> outputsAll = new HashMap<>();
    private Map<Long, Long> groupsCount = new HashMap<>();
    private Map<Long, Long> groupsCountUnique = new HashMap<>();
    private Map<Long, Map<String, BigDecimal>> groupsPositiveCountAll = new HashMap<>();
    private Map<Long, Map<String, BigDecimal>> groupsPositiveCountUniqueAll = new HashMap<>();
    private Map<Long, Map<String, Long>> groupsNegativeCountAll = new HashMap<>();
    private Map<Long, Map<String, Long>> groupsNegativeCountUniqueAll = new HashMap<>();
    private List<Pair<Long, Long>> minMaxKeys = new ArrayList<>();

    private List<String> groupNames;

    private String pathToFolderWithResults;
    private static final Long MIN_KEYS[] = {1L, 2L, 10L, 100L};

    private DecimalFormat formatter;

    public HistoricalBatchesStatisticsAggregator(List<String> groupNames, String pathToFolderWithResults) {
        this.groupNames = groupNames;
        this.pathToFolderWithResults = pathToFolderWithResults;

        // Create containers for statistics
        outputsAll = new HashMap<>(); // TODO unused
        groupsCount = new HashMap<>();
        groupsCountUnique = new HashMap<>();
        groupsPositiveCountAll = new HashMap<>();
        groupsPositiveCountUniqueAll = new HashMap<>();
        groupsNegativeCountAll = new HashMap<>();
        groupsNegativeCountUniqueAll = new HashMap<>();
        minMaxKeys = new ArrayList<>();

        DecimalFormatSymbols decimalFormatter = new DecimalFormatSymbols();
        decimalFormatter.setDecimalSeparator('.');
        formatter = new DecimalFormat("#0.00000000", decimalFormatter);

        //Initialize containers
        for (int i = 0; i < MIN_KEYS.length; i++) {
            Long minKey = MIN_KEYS[i], maxKey = null;
            if (i < MIN_KEYS.length - 1) maxKey = MIN_KEYS[i + 1];
            minMaxKeys.add(new Pair<>(minKey, maxKey));

            groupsCount.put(minKey, 0L);
            groupsCountUnique.put(minKey, 0L);

            Map<String, BigDecimal> groupsPositiveCount = new TreeMap<>();
            Map<String, BigDecimal> groupsPositiveCountUnique = new TreeMap<>();
            Map<String, Long> groupsNegativeCount = new TreeMap<>();
            Map<String, Long> groupsNegativeCountUnique = new TreeMap<>();
            for (String groupName : groupNames) {
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
    }

    @Override
    public void addStatistics(ClassificationContainer container) {
        long allKeys = container.getNumOfAllKeys();
        long uniqueKeys = container.getNumOfUniqueKeys();

        for (Pair<Long, Long> minMaxKey : minMaxKeys) {
            Long minKey = minMaxKey.getKey(), maxKey = minMaxKey.getValue();

            if (container.getNumOfUniqueKeys() < minKey) continue;
            if (maxKey != null) {
                if (container.getNumOfUniqueKeys() >= maxKey) continue;
            }

            groupsCount.put(minKey, groupsCount.get(minKey) + container.getNumOfAllKeys());
            groupsCountUnique.put(minKey, groupsCountUnique.get(minKey) + container.getNumOfUniqueKeys());

            Map<String, BigDecimal> groupsPositiveCount = groupsPositiveCountAll.get(minKey);
            Map<String, BigDecimal> groupsPositiveCountUnique = groupsPositiveCountUniqueAll.get(minKey);
            Map<String, Long> groupsNegativeCount = groupsNegativeCountAll.get(minKey);
            Map<String, Long> groupsNegativeCountUnique = groupsNegativeCountUniqueAll.get(minKey);

            for (String groupName : groupNames) {
                BigDecimal val = container.getRow().getSource(groupName);
                if (val != null) {
                    groupsPositiveCount.put(groupName, groupsPositiveCount.get(groupName).add(val.multiply(BigDecimal.valueOf(allKeys))));
                    groupsPositiveCountUnique.put(groupName, groupsPositiveCountUnique.get(groupName).add(val.multiply(BigDecimal.valueOf(uniqueKeys))));
                } else {
                    groupsNegativeCount.put(groupName, groupsNegativeCount.get(groupName) + allKeys);
                    groupsNegativeCountUnique.put(groupName, groupsNegativeCountUnique.get(groupName) + uniqueKeys);
                }
            }
        }
    }

    @Override
    public void savePriorProbabilitySummary(PriorProbabilityEstimator estimator) {
        ExtendedWriter writer = null;
        try {
            writer = new ExtendedWriter(new File(pathToFolderWithResults, "prior_probability.json"));
            writer.writeln(estimator.summaryToJSON().toString());
            writer.close();
        } catch (IOException e) {
            System.err.println("Error while writing prior probability results.");
        }
    }

    @Override
    public void saveStatistics() {
        Template classificationResult;
        try {
            classificationResult = new Template("classificationResult.csv");
        } catch (IOException ex) {
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

            String file = "results, " + minKey + (maxKey != null ? " - " + String.valueOf(maxKey - 1) : " and more") + " keys.csv";
            try (ExtendedWriter writer = new ExtendedWriter(new File(pathToFolderWithResults, file))) {
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

    private Pair<String, String> positiveVectorToCsv(Map<String, BigDecimal> vector, Long count) {
        String groups = "", values = "";
        for (Map.Entry<String, BigDecimal> entry : vector.entrySet()) {
            groups += "," + entry.getKey();
            if (count == 0L) values += ",-";
            else
                values += "," + formatter.format(entry.getValue().divide(BigDecimal.valueOf(count), BigDecimal.ROUND_CEILING).doubleValue());
        }
        return new Pair<>(groups, values);
    }

    private Pair<String, String> negativeVectorToCsv(Map<String, Long> vector) {
        String groups = "", values = "";
        for (Map.Entry<String, Long> entry : vector.entrySet()) {
            groups += "," + entry.getKey();
            values += "," + entry.getValue();
        }
        return new Pair<>(groups, values);
    }
}
