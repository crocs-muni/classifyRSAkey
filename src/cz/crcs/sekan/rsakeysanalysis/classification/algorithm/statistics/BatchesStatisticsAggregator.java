package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKeyStub;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * @author xnemec1
 * @version 11/24/16.
 */
public class BatchesStatisticsAggregator implements StatisticsAggregator {

    private static final String SEPARATOR = ",";

    private List<String> groupNames;

    private String pathToFolderWithResults;

    private DecimalFormat formatter;

    private Map<Long, BatchStatistic> positiveUniqueBatches;
    private Map<Long, BatchStatistic> positiveDuplicateBatches;
    private Map<Long, BatchStatistic> negativeUniqueBatches;
    private Map<Long, BatchStatistic> negativeDuplicateBatches;

    public BatchesStatisticsAggregator(List<String> groupNames, String pathToFolderWithResults) {
        this.groupNames = groupNames;
        this.pathToFolderWithResults = pathToFolderWithResults;
        DecimalFormatSymbols decimalFormatter = new DecimalFormatSymbols();
        decimalFormatter.setDecimalSeparator('.');
        formatter = new DecimalFormat("#0.00000000", decimalFormatter);

        positiveUniqueBatches = new TreeMap<>();
        positiveDuplicateBatches = new TreeMap<>();
        negativeUniqueBatches = new TreeMap<>();
        negativeDuplicateBatches = new TreeMap<>();
    }

    @Override
    public void addStatistics(ClassificationContainer container, ClassificationKeyStub... keyStubs) {
        long batchSize = container.getNumOfUniqueKeys();

        BatchStatistic modified = positiveUniqueBatches.getOrDefault(batchSize, new BatchStatistic(batchSize,
                BatchStatistic.ClassificationType.POSITIVE, BatchStatistic.DuplicityType.UNIQUE, groupNames));
        modified.addBatchStatistics(container);
        positiveUniqueBatches.put(batchSize, modified);

        modified = negativeUniqueBatches.getOrDefault(batchSize, new BatchStatistic(batchSize,
                BatchStatistic.ClassificationType.NEGATIVE, BatchStatistic.DuplicityType.UNIQUE, groupNames));
        modified.addBatchStatistics(container);
        negativeUniqueBatches.put(batchSize, modified);

        batchSize = container.getNumOfAllKeys();

        modified = positiveDuplicateBatches.getOrDefault(batchSize, new BatchStatistic(batchSize,
                BatchStatistic.ClassificationType.POSITIVE, BatchStatistic.DuplicityType.DUPLICATE, groupNames));
        modified.addBatchStatistics(container);
        positiveDuplicateBatches.put(batchSize, modified);

        modified = negativeDuplicateBatches.getOrDefault(batchSize, new BatchStatistic(batchSize,
                BatchStatistic.ClassificationType.NEGATIVE, BatchStatistic.DuplicityType.DUPLICATE, groupNames));
        modified.addBatchStatistics(container);
        negativeDuplicateBatches.put(batchSize, modified);
    }

    @Override
    public void saveStatistics() {
        List<Map<Long, BatchStatistic>> allBatchMaps = new ArrayList<>(4);
        allBatchMaps.add(positiveUniqueBatches);
        allBatchMaps.add(negativeUniqueBatches);
        allBatchMaps.add(positiveDuplicateBatches);
        allBatchMaps.add(negativeDuplicateBatches);

        try (ExtendedWriter writer = new ExtendedWriter(new File(pathToFolderWithResults, "individual_statistics.csv"))) {
            writer.writeln(BatchStatistic.rowStatisticHeader(groupNames, SEPARATOR));
            for (Map<Long, BatchStatistic> map : allBatchMaps) {
                for (Map.Entry<Long, BatchStatistic> entry : map.entrySet()) {
                    if (entry.getValue().isPositive()) {
                        writer.writeln(entry.getValue().normalizedCopy().toRowStatistic(groupNames, formatter, SEPARATOR));
                    } else {
                        writer.writeln(entry.getValue().toRowStatistic(groupNames, formatter, SEPARATOR));
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("Error while writing individual statistics");
        }

        try (ExtendedWriter writer = new ExtendedWriter(new File(pathToFolderWithResults, "overall_statistics.csv"))) {
            writer.writeln(BatchStatistic.rowStatisticHeader(groupNames, SEPARATOR));
            for (Map<Long, BatchStatistic> map : allBatchMaps) {
                BatchStatistic combinedStatistic = BatchStatistic.combineBatches(new ArrayList<>(map.values()), groupNames);
                writer.writeln(combinedStatistic.toRowStatistic(groupNames, formatter, SEPARATOR));
            }
        } catch (IOException ex) {
            System.err.println("Error while writing combined statistics");
            ex.printStackTrace(System.err);
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
}
