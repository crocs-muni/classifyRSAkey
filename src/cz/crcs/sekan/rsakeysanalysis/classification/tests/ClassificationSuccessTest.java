package cz.crcs.sekan.rsakeysanalysis.classification.tests;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.Classification;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.ClassificationConfiguration;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.*;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.key.property.ModulusHashPropertyExtractor;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.util.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.*;

import static cz.crcs.sekan.rsakeysanalysis.classification.algorithm.Classification.BatchType.MODULUS_HASH;

/**
 * @author xnemec1
 * @version 2/28/17.
 */
public class ClassificationSuccessTest {

    private static final String INTERCEPT_GROUP_NAME = "Intercept";

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

    public static void overallSuccess(ClassificationConfiguration originalConfiguration) throws NoSuchAlgorithmException, IOException, DataSetException {
        SecureRandom random = originalConfiguration.configureRandom();

        int repetitionCount = 100; // TODO

        List<Double> noiseLevels = new ArrayList<>(Arrays.asList(0d, 0.005d, 0.01d, 0.015d, 0.02d, 0.03d, 0.04d, 0.05d));

        PriorProbability tlsProbability = new PriorProbability();
        tlsProbability.put("Group  1", BigDecimal.valueOf(0));
        tlsProbability.put("Group  2", BigDecimal.valueOf(0));
        tlsProbability.put("Group  3", BigDecimal.valueOf(0.003576061038810584));
        tlsProbability.put("Group  4", BigDecimal.valueOf(0.0003927594581238341));
        tlsProbability.put("Group  5", BigDecimal.valueOf(0.0002483442363191982));
        tlsProbability.put("Group  6", BigDecimal.valueOf(0.7005150121166919));
        tlsProbability.put("Group  7", BigDecimal.valueOf(0.014414690438664855));
        tlsProbability.put("Group  8", BigDecimal.valueOf(0));
        tlsProbability.put("Group  9", BigDecimal.valueOf(0.0002920968495064412));
        tlsProbability.put("Group 10", BigDecimal.valueOf(0.0002097855460252638));
        tlsProbability.put("Group 11", BigDecimal.valueOf(0.1932342116456942));
        tlsProbability.put("Group 12", BigDecimal.valueOf(0.0433618751456715));
        tlsProbability.put("Group 13", BigDecimal.valueOf(0.04375516352449228));

        Map<PriorProbability.Distribution, Map<Double, Double>> expectedDifferences = new TreeMap<>();
        Map<PriorProbability.Distribution, Map<Double, Double>> expectedWorstDifferences = new TreeMap<>();
        Map<PriorProbability.Distribution, Map<Double, ClassificationSuccessStatistic>> successStatistics = new TreeMap<>();

        for (PriorProbability.Distribution distribution : PriorProbability.Distribution.values()) {
            expectedDifferences.put(distribution, new TreeMap<>());
            expectedWorstDifferences.put(distribution, new TreeMap<>());
            successStatistics.put(distribution, new TreeMap<>());
        }

        List<String> groupNames = new ArrayList<>(originalConfiguration.classificationTable.getGroupsNames());

        for (double randomNoise : noiseLevels) {

            for (PriorProbability.Distribution distribution : PriorProbability.Distribution.values()) {

                PriorProbability trialWeigh = new PriorProbability();
                Map<String, ClassificationSuccessStatistic> statistics = new HashMap<>();

                DecimalFormat formatter = null;

                List<Double> worstDifferences = new ArrayList<>(repetitionCount);

                DescriptiveStatistics allDifferences = new DescriptiveStatistics();

                for (Integer i = 0; i < repetitionCount; i++) {
                    ClassificationConfiguration configuration = originalConfiguration.deepCopy();
                    ClassificationTable table = configuration.classificationTable.makeCopy();

                    ClassificationTable tableForSimulation = table.makeCopy();

                    PriorProbability randomProbability;

                    switch (distribution) {
                        case EVEN:
                            randomProbability = PriorProbability.uniformProbability(groupNames);
                            break;
                        case UNIFORM:
                            randomProbability = PriorProbability.randomize(random, groupNames);
                            break;
                        case GEOMETRIC:
                            randomProbability = PriorProbability.randomizeGeometric(random, groupNames,
                                    BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.025));
                            break;
                        case CUSTOM:
                            randomProbability = PriorProbability.randomizeFromPrior(random, tlsProbability,
                                    BigDecimal.valueOf(0.03));
                            break;
                        default:
                            throw new RuntimeException("Bad distribution type");
                    }

                    configuration.batchType = MODULUS_HASH;

                    configuration.makeOutputs = false;

                    Classification.Builder builder = Classification.BuildHelper.prepareBuilder(configuration, null);

                    int sampleSize = configuration.keyCount;

                    ClassificationSuccessStatisticsAggregator aggregator = new ClassificationSuccessStatisticsAggregator(
                            new ArrayList<String>(tableForSimulation.getGroupsNames()),
                            configuration.outputFolderPath, configuration.rngSeed);

                    PriorProbability randomProbabilityWithNoise = randomProbability.makeCopy();

                    if (randomNoise > 0d) {
                        tableForSimulation = addRandomGroupToTable(tableForSimulation, random);
                        randomProbabilityWithNoise.put(INTERCEPT_GROUP_NAME,
                                BigDecimal.valueOf(randomNoise / (1 - randomNoise)));
                        randomProbabilityWithNoise = randomProbabilityWithNoise.normalized();
                    }

                    builder.setDataSetIterator(SimulatedDataSetIterator.fromClassificationTable(tableForSimulation,
                            randomProbabilityWithNoise, sampleSize, random));
                    builder.setStatisticsAggregator(aggregator);
                    builder.setPriorProbabilityEstimator(new NonNegativeLeastSquaresFitPriorProbabilityEstimator(table.makeCopy()));
                    //builder.setPriorProbabilityEstimator(new UniformPriorProbabilityEstimator(table.makeCopy()));
                    //builder.setPriorProbabilityEstimator(new UserDefinedPriorProbabilityEstimator(randomProbability));
                    PriorProbabilityEstimator priorProbabilityEstimator = builder.build().classify();

                    PriorProbability estimatedProbability = priorProbabilityEstimator.computePriorProbability();

                    List<Double> differences = DistributionsComparator.allDifferences(
                            randomProbability.toDoubleArray(groupNames), estimatedProbability.toDoubleArray(groupNames));

                    if (differences != null) {
                        differences.forEach(allDifferences::addValue);
                        double largestDifference = differences.stream().max(Double::compare).orElseGet(() -> 1d);

                        worstDifferences.add(largestDifference);
                    } else {
                        System.err.println("Failed to compute difference of distributions");
                    }

                    formatter = aggregator.getFormatter();

                    ClassificationSuccessStatistic statistic =
                            ClassificationSuccessStatistic.weighedAverage(
                                    aggregator.getGroupNameToStatistic(),
                                    randomProbabilityWithNoise, // comparing to the probability with noise, the noise should have always worst guess
                                    groupNames.size() + 1, true);

                    trialWeigh.put(i.toString(), BigDecimal.ONE);
                    statistics.put(i.toString(), statistic);
                }

                System.out.println(String.format("Type of distribution: %s; Random noise: %1.4f", distribution, randomNoise));

                ClassificationSuccessStatistic finalStatistic =
                        ClassificationSuccessStatistic.weighedAverage(statistics, trialWeigh, groupNames.size() + 1, true);
                System.out.println(ClassificationSuccessStatistic.toRowStatistic(
                        "Weighed average  ", finalStatistic.toProbability(groupNames.size() + 1, true), formatter, ","));
                System.out.println(ClassificationSuccessStatistic.toRowStatistic(
                        "Cumulated average", finalStatistic.toCumulativeDistribution(groupNames.size() + 1, true), formatter, ","));

                successStatistics.get(distribution).put(randomNoise, finalStatistic);


                double worstDifference = worstDifferences.stream().max(Double::compare).orElseGet(() -> 1d);

                DescriptiveStatistics diffStatistics = new DescriptiveStatistics();
                worstDifferences.forEach(diffStatistics::addValue);
                System.out.println("Worst differences for each run:");
                System.out.print(String.format("mean=%1.3f;", diffStatistics.getMean()));
                StringBuilder builder = new StringBuilder();
                int percentileStep = 5;
                for (int percentile = percentileStep; percentile <= 100; percentile += percentileStep) {
                    builder.append(String.format("%1.3f;", diffStatistics.getPercentile(percentile)));
                }
                System.out.println(builder.toString());

                expectedWorstDifferences.get(distribution).put(randomNoise, diffStatistics.getMean());


                System.out.println("All differences across all runs:");
                System.out.print(String.format("mean=%1.3f;", allDifferences.getMean()));
                builder = new StringBuilder();
                for (int percentile = percentileStep; percentile <= 100; percentile += percentileStep) {
                    builder.append(String.format("%1.3f;", allDifferences.getPercentile(percentile)));
                }
                System.out.println(builder.toString());

                expectedDifferences.get(distribution).put(randomNoise, allDifferences.getMean());

                System.out.println(String.format("worst case: %1.3f", allDifferences.getMax()));
            }
        }

        System.out.println();
        String sep = ",";

        System.out.println("Distribution,Expected error based on the amount of random noise,,,,,,,,Expected worst error based on the amount of random noise,,,,,,,,");
        System.out.println(String.format("%s%s%s%s", sep, noiseLevels, sep, noiseLevels));
        for (PriorProbability.Distribution distribution : PriorProbability.Distribution.values()) {
            System.out.print(distribution + sep);
            for (boolean expected : new boolean[]{true, false}) {
                for (double randomNoise : noiseLevels) {
                    double val;
                    if (expected) {
                        val = expectedDifferences.get(distribution).get(randomNoise);
                    } else {
                        // worst
                        val = expectedWorstDifferences.get(distribution).get(randomNoise);
                    }
                    System.out.print(String.format("%1.5f%s", val, sep));
                }
            }
            System.out.println();
        }

        System.out.println();
        System.out.println("Distribution,Noise,,,,,,,,,,,,,,,,,,,,,,,");
        StringBuilder noiseHeader = new StringBuilder();
        for (double randomNoise : noiseLevels) {
            noiseHeader.append(",");
            noiseHeader.append(randomNoise);
            noiseHeader.append(",,");
        }
        System.out.println(noiseHeader.toString());
        noiseHeader = new StringBuilder();
        for (int i = 0; i < noiseLevels.size(); i++) {
            noiseHeader.append(",1,2,3");
        }
        System.out.println(noiseHeader.toString());

        for (PriorProbability.Distribution distribution : PriorProbability.Distribution.values()) {
            System.out.print(distribution + sep);
            for (double randomNoise : noiseLevels) {
                List<BigDecimal> val = successStatistics.get(distribution).get(randomNoise).toCumulativeDistribution(groupNames.size() + 1, true);

                for (int guessOrder : new int[]{1, 2, 3}) {
                    System.out.print(String.format("%1.5f%s", val.get(guessOrder-1), sep));
                }
            }
            System.out.println();
        }
    }

    public static void groupSuccess(ClassificationConfiguration originalConfiguration) throws NoSuchAlgorithmException, IOException, DataSetException {
        SecureRandom random = originalConfiguration.configureRandom();

        Map<String, ClassificationSuccessStatistic> groupNameToStatistic = new TreeMap<>();

        List<String> groupNames = new ArrayList<>(originalConfiguration.classificationTable.getGroupsNames());

        DecimalFormat formatter = null;

        for (String groupName : groupNames) {
            ClassificationConfiguration configuration = originalConfiguration.deepCopy();

            configuration.batchType = MODULUS_HASH;

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
            builder.setPriorProbabilityEstimator(
//                    new NonNegativeLeastSquaresFitPriorProbabilityEstimator(configuration.classificationTable.makeCopy()));
                    new UniformPriorProbabilityEstimator(configuration.classificationTable.makeCopy()));
            PriorProbabilityEstimator priorProbabilityEstimator = builder.build().classify();

            System.out.println(priorProbabilityEstimator.computePriorProbability().toJSON());

            formatter = aggregator.getFormatter();

            groupNameToStatistic.put(groupName, aggregator.getGroupNameToStatistic().get(groupName));

            System.out.println(ClassificationSuccessStatistic.toRowStatistic(groupName,
                    groupNameToStatistic.get(groupName).toProbability(groupNames.size(), false), formatter, ","));
        }
        ClassificationSuccessStatisticsAggregator.saveStatisticsToFiles(groupNames, groupNameToStatistic,
                originalConfiguration.outputFolderPath, originalConfiguration.rngSeed, formatter);
        ClassificationSuccessStatistic finalStatistic =
                ClassificationSuccessStatistic.weighedAverage(groupNameToStatistic,
                        PriorProbability.uniformProbability(groupNames), groupNames.size() + 1, true);
        System.out.println(ClassificationSuccessStatistic.toRowStatistic(
                "Weighed average  ", finalStatistic.toProbability(groupNames.size() + 1, true), formatter, ","));
        System.out.println(ClassificationSuccessStatistic.toRowStatistic(
                "Cumulated average", finalStatistic.toCumulativeDistribution(groupNames.size() + 1, true), formatter, ","));

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

    private static ClassificationTable addRandomGroupToTable(ClassificationTable table, Random random) {
        List<String> masks = table.getMasks();
        List<BigDecimal> probabilities = new ArrayList<>(masks.size());
        Map<String, BigDecimal> maskProbabilities = new TreeMap<>();
        for (int i = 0; i < masks.size(); i++) {
            probabilities.add(BigDecimal.valueOf(random.nextDouble()));
        }
        BigDecimal sum = probabilities.stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        if (!BigDecimal.ZERO.equals(sum)) {
            probabilities.replaceAll(probability -> probability.divide(sum, PriorProbability.BIG_DECIMAL_SCALE, BigDecimal.ROUND_HALF_EVEN));
        }
        for (int i = 0; i < masks.size(); i++) {
            maskProbabilities.put(masks.get(i), probabilities.get(i));
        }

        ClassificationTable modifiedTable = table.makeCopy();
        modifiedTable.addSource(INTERCEPT_GROUP_NAME, maskProbabilities);
        return modifiedTable;
    }
}
