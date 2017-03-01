package cz.crcs.sekan.rsakeysanalysis.classification.tests.util;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics.BatchStatistic;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics.StatisticsAggregator;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKeyStub;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author xnemec1
 * @version 2/28/17.
 */
public class ClassificationSuccessStatisticsAggregator implements StatisticsAggregator {

    private static final String SEPARATOR = ",";

    private Map<String, ClassificationSuccessStatistic> groupNameToStatistic;
    private List<String> groupNames;

    private DecimalFormat formatter;

    private String pathToFolderWithResults;
    private Long randomGeneratorSeed;

    public static DecimalFormat defaultFormatter() {
        DecimalFormatSymbols decimalFormatter = new DecimalFormatSymbols();
        decimalFormatter.setDecimalSeparator('.');
        return new DecimalFormat("#0.00000000", decimalFormatter);
    }

    public ClassificationSuccessStatisticsAggregator(List<String> groupNames, String pathToFolderWithResults, Long randomGeneratorSeed) {
        this.groupNames = groupNames;
        this.groupNameToStatistic = new TreeMap<>();
        this.randomGeneratorSeed = randomGeneratorSeed;
        this.pathToFolderWithResults = pathToFolderWithResults;

        formatter = defaultFormatter();
    }

    @Override
    public void addStatistics(ClassificationContainer container, ClassificationKeyStub... keyStubs) {
        List<String> mostProbableGroups = container.getRow().getTopGroups(ClassificationRow.ALL_GROUPS);
        String groupName = null;
        for (ClassificationKeyStub stub : keyStubs) {
            if (stub.getRealSource() == null) {
                throw new IllegalArgumentException("getRealSource() != null needed to check if guess is correct");
            }
            if (groupName == null) {
                groupName = stub.getRealSource();
            } else if (!groupName.equals(stub.getRealSource())) {
                throw new IllegalArgumentException("Classification container contains batch with different sources");
            }
        }
        int index = mostProbableGroups.indexOf(groupName);
        ClassificationSuccessStatistic statistic = groupNameToStatistic.getOrDefault(groupName, new ClassificationSuccessStatistic());
        statistic.addGuessWithOrder(index);
        groupNameToStatistic.put(groupName, statistic);
    }

    @Override
    public void saveStatistics() {
        saveStatisticsToFiles(groupNames, groupNameToStatistic, pathToFolderWithResults, randomGeneratorSeed, formatter);
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

    public Map<String, ClassificationSuccessStatistic> getGroupNameToStatistic() {
        return groupNameToStatistic;
    }

    public static void saveStatisticsToFiles(List<String> groupNames, Map<String, ClassificationSuccessStatistic> groupNameToStatistic,
                                             String pathToFolderWithResults, Long randomGeneratorSeed, DecimalFormat formatter) {
        int groupCount = groupNames.size();

        List<Integer> guessHeader = new ArrayList<>(groupCount + 1);
        for (int guesses = 1; guesses <= groupCount + 1; guesses++) {
            guessHeader.add(guesses == groupCount + 1 ? -1 : guesses);
        }

        try (ExtendedWriter probabilityWriter = new ExtendedWriter(new File(pathToFolderWithResults, "success_probabilities.csv"));
             ExtendedWriter cumulativeWriter = new ExtendedWriter(new File(pathToFolderWithResults, "success_cumulative.csv"))) {

            probabilityWriter.writeln(ClassificationSuccessStatistic.rowStatisticHeader(randomGeneratorSeed, guessHeader, SEPARATOR));
            cumulativeWriter.writeln(ClassificationSuccessStatistic.rowStatisticHeader(randomGeneratorSeed, guessHeader, SEPARATOR));

            for (String groupName : groupNames) {
                ClassificationSuccessStatistic statistic = groupNameToStatistic.get(groupName);
                if (statistic != null) {
                    List<BigDecimal> probabilities = statistic.toProbability(groupCount, false);
                    List<BigDecimal> cumulative = statistic.toCumulativeDistribution(groupCount, false);
                    probabilityWriter.writeln(ClassificationSuccessStatistic.toRowStatistic(groupName, probabilities, formatter, SEPARATOR));
                    cumulativeWriter.writeln(ClassificationSuccessStatistic.toRowStatistic(groupName, cumulative, formatter, SEPARATOR));
                }
            }
        } catch (IOException ex) {
            System.err.println("Error while writing classification success statistics");
            ex.printStackTrace(System.err);
        }
    }

    public DecimalFormat getFormatter() {
        return formatter;
    }
}
