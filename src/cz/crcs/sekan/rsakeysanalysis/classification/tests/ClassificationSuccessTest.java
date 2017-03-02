package cz.crcs.sekan.rsakeysanalysis.classification.tests;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.Classification;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.ClassificationConfiguration;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbability;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.util.ClassificationSuccessStatistic;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.util.ClassificationSuccessStatisticsAggregator;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.util.SimulatedDataSetIterator;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author xnemec1
 * @version 2/28/17.
 */
public class ClassificationSuccessTest {

    public static void runFromConfiguration(ClassificationConfiguration configuration)
            throws IOException, DataSetException, NoSuchAlgorithmException {
        Classification.Builder builder = Classification.BuildHelper.prepareBuilder(configuration, null);

        SecureRandom random = configuration.configureRandom();

        builder.setDataSetIterator(SimulatedDataSetIterator.fromClassificationTable(configuration.classificationTable,
                configuration.priorProbability, configuration.keyCount, random));
        builder.setStatisticsAggregator(new ClassificationSuccessStatisticsAggregator(
                new ArrayList<String>(configuration.classificationTable.getGroupsNames()),
                configuration.outputFolderPath, configuration.rngSeed));

        builder.build().classify();
    }

    public static void groupSuccess(ClassificationConfiguration originalConfiguration) throws NoSuchAlgorithmException, IOException, DataSetException {
        SecureRandom random = originalConfiguration.configureRandom();

        Map<String, ClassificationSuccessStatistic> groupNameToStatistic = new TreeMap<>();

        List<String> groupNames = new ArrayList<>(originalConfiguration.classificationTable.getGroupsNames());

        DecimalFormat formatter = null;

        for (String groupName : groupNames) {
            ClassificationConfiguration configuration = originalConfiguration.deepCopy();

            Classification.Builder builder = Classification.BuildHelper.prepareBuilder(configuration, null);
            PriorProbability onlyOneGroup = new PriorProbability();
            for (String name : groupNames) {
                onlyOneGroup.setGroupProbability(name, BigDecimal.ZERO);
            }
            onlyOneGroup.setGroupProbability(groupName, BigDecimal.ONE);
            builder.setDataSetIterator(SimulatedDataSetIterator.fromClassificationTable(configuration.classificationTable,
                    onlyOneGroup, configuration.keyCount, random));
            ClassificationSuccessStatisticsAggregator aggregator = new ClassificationSuccessStatisticsAggregator(
                    new ArrayList<String>(configuration.classificationTable.getGroupsNames()),
                    configuration.outputFolderPath, configuration.rngSeed);
            builder.setStatisticsAggregator(aggregator);
            builder.build().classify();

            formatter = aggregator.getFormatter();

            groupNameToStatistic.put(groupName, aggregator.getGroupNameToStatistic().get(groupName));

            System.out.println(ClassificationSuccessStatistic.toRowStatistic(groupName,
                    groupNameToStatistic.get(groupName).toProbability(groupNames.size(), false), formatter, ","));
        }
        ClassificationSuccessStatisticsAggregator.saveStatisticsToFiles(groupNames, groupNameToStatistic,
                originalConfiguration.outputFolderPath, originalConfiguration.rngSeed, formatter);
    }

    public static void theoreticalSuccess(ClassificationConfiguration configuration) {
        ClassificationTable table = configuration.classificationTable;
        // all groups must be normalized to the same sum

        // order of guess depends on prior probability, but do NOT normalize the row, since that breaks probabilities
        table.applyPriorProbability(configuration.priorProbability, false);

        List<String> groupNames = new ArrayList<>(table.getGroupsNames());

        Map<String, Map<Integer, BigDecimal>> groupToGuessCountToProbability = new TreeMap<>();

        for (String groupName : groupNames) {
            groupToGuessCountToProbability.put(groupName, new TreeMap<>());
        }

        for (String mask : table.getMasks()) {
            ClassificationRow row = table.getTable().get(mask);
            List<String> sortedGroups = row.getTopGroups(ClassificationRow.ALL_GROUPS);
            for (String groupName : table.getGroupsNames()) {
                BigDecimal maskProbabilityInGroup = row.getSource(groupName);
                groupToGuessCountToProbability.get(groupName).compute(sortedGroups.indexOf(groupName),
                        (key, oldValue) -> oldValue == null ? maskProbabilityInGroup : oldValue.add(maskProbabilityInGroup));
            }
        }

        Map<String, ClassificationSuccessStatistic> groupNameToStatistic = new TreeMap<>();
        for (String groupName : groupNames) {
            groupNameToStatistic.put(groupName, new ClassificationSuccessStatistic(groupToGuessCountToProbability.get(groupName)));
        }

        ClassificationSuccessStatisticsAggregator.saveStatisticsToFiles(groupNames, groupNameToStatistic,
                configuration.outputFolderPath, configuration.rngSeed, ClassificationSuccessStatisticsAggregator.defaultFormatter());
    }
}
