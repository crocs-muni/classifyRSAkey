package cz.crcs.sekan.rsakeysanalysis.classification.tests;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.Classification;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.ClassificationConfiguration;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.NonNegativeLeastSquaresFitPriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbability;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.UserDefinedPriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset.NoActionDataSetSaver;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics.BatchStatistic;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics.BatchesStatisticsAggregator;
import cz.crcs.sekan.rsakeysanalysis.classification.key.property.SourcePropertyExtractor;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.util.SimulatedDataSetIterator;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

/**
 * @author xnemec1
 * @version 3/2/17.
 */
public class AprioriTest {

    private static final String INTERCEPT_GROUP_NAME = "Intercept";
    private static final double MAX_INTERCEPT_PROBABILITY = 0.05;
    private static final BigDecimal MAX_ESTIMATE_ERROR = BigDecimal.valueOf(0.05d);

    /**
     * This test simulates random prior probabilities over the classification table in the supplied configuration
     * and measures how the estimated probability matches the used one.
     * Includes a test with added random source, which aims to simulate distributions of unknown sources.
     * @param configuration
     * @throws NoSuchAlgorithmException
     */
    public static void testEstimatePrecision(ClassificationConfiguration configuration) throws NoSuchAlgorithmException {

        List<String> groupNames = new ArrayList<>(configuration.classificationTable.getGroupsNames());

        SecureRandom random = configuration.configureRandom();
        System.out.println("Experiment seed: " + configuration.rngSeed);

        PriorProbability uniform = PriorProbability.uniformProbability(groupNames);

        BigDecimal sumDistanceEstimated = BigDecimal.ZERO;
        BigDecimal sumDistanceUniform = BigDecimal.ZERO;

        for (boolean addRandomSource : new boolean[]{false, true}) {

            for (int i = 0; i < configuration.keyCount; i++) {
                ClassificationTable tableForSimulation = configuration.classificationTable.makeCopy();

                PriorProbability randomProbability;

                if (addRandomSource) {
                    tableForSimulation = addRandomGroupToTable(tableForSimulation, random);
                    randomProbability = PriorProbability.randomize(random, new ArrayList<>(tableForSimulation.getGroupsNames()));
                    randomProbability.put(INTERCEPT_GROUP_NAME, BigDecimal.valueOf(random.nextDouble() * MAX_INTERCEPT_PROBABILITY));
                    randomProbability = randomProbability.normalized();
                } else {
                    randomProbability = PriorProbability.randomize(random, groupNames);
                }

                Map<String, BigDecimal> simulatedStatistic =
                        simulateStatistics(tableForSimulation, randomProbability.makeCopy());
                PriorProbabilityEstimator estimator = new NonNegativeLeastSquaresFitPriorProbabilityEstimator(configuration.classificationTable.makeCopy());
                estimator.setMaskToFrequency(simulatedStatistic);
                PriorProbability estimatedProbability = estimator.computePriorProbability();

                if (addRandomSource) {
                    randomProbability.remove(INTERCEPT_GROUP_NAME);
                    randomProbability = randomProbability.normalized();
                }

                BigDecimal distance = randomProbability.distance(estimatedProbability);
                sumDistanceEstimated = sumDistanceEstimated.add(distance);
                sumDistanceUniform = sumDistanceUniform.add(randomProbability.distance(uniform));
            }

            if (addRandomSource) {
                System.out.println("\nA completely random group is added to simulate unknown groups which can cover up to "
                        + MAX_INTERCEPT_PROBABILITY * 100 + "% of the simulated dataset");
            }
            System.out.println("Average distance from random prior distribution:");
            System.out.println(String.format("    to estimated: %s",
                    sumDistanceEstimated.divide(BigDecimal.valueOf(configuration.keyCount),
                            PriorProbability.BIG_DECIMAL_SCALE, BigDecimal.ROUND_HALF_EVEN)));
            System.out.println(String.format("    to uniform:   %s",
                    sumDistanceUniform.divide(BigDecimal.valueOf(configuration.keyCount),
                            PriorProbability.BIG_DECIMAL_SCALE, BigDecimal.ROUND_HALF_EVEN)));
        }
    }

    /**
     * This test checks if the prior probability shapes the classification result
     */
    public static void testPriorInfluence(ClassificationConfiguration configuration) throws NoSuchAlgorithmException {
        SecureRandom random = configuration.configureRandom();
        System.out.println("Experiment seed: " + configuration.rngSeed);
        ClassificationTable table = configuration.classificationTable;
        int repetitionCount = configuration.keyCount;

        // I sanity check -- simulate dataset with random prior probability, use the same probability for classification
        System.out.println("==== Classification prior probability is the same as dataset prior probability ====");
        for (int i = 0; i < repetitionCount; i++) {
            PriorProbability randomProbability = PriorProbability.randomize(random, new ArrayList<>(table.getGroupsNames()));
            classifyRandom(random, table.makeCopy(), randomProbability, randomProbability);
        }

        // II sanity check -- simulate dataset with random prior probability, estimate and classify
        System.out.println("==== Classification prior probability is estimated from the dataset ====");
        for (int i = 0; i < repetitionCount; i++) {
            PriorProbability randomProbability = PriorProbability.randomize(random, new ArrayList<>(table.getGroupsNames()));
            classifyRandom(random, table.makeCopy(), randomProbability, null);
        }

        // III -- simulate dataset with random prior probability, use slightly wrong prior probability estimate
        System.out.println("==== Classification prior probability is slightly off from the dataset prior probability ===="); // TODO repeat for different distances
        for (int i = 0; i < repetitionCount; i++) {
            PriorProbability randomProbability = PriorProbability.randomize(random, new ArrayList<>(table.getGroupsNames()));
            PriorProbability badEstimate = PriorProbability.randomize(random, new ArrayList<>(table.getGroupsNames()))
                    .scale(MAX_ESTIMATE_ERROR).sum(randomProbability).normalized();
            classifyRandom(random, table.makeCopy(), randomProbability, badEstimate);
        }

        // IV -- simulate dataset with random prior probability, use uniform prior probability estimate
        System.out.println("==== Classification prior probability is uniform ====");
        for (int i = 0; i < repetitionCount; i++) {
            PriorProbability randomProbability = PriorProbability.randomize(random, new ArrayList<>(table.getGroupsNames()));
            PriorProbability uniformEstimate = PriorProbability.uniformProbability(new ArrayList<>(table.getGroupsNames()));
            classifyRandom(random, table.makeCopy(), randomProbability, uniformEstimate);
        }

        // V -- simulate dataset with random prior probability, use wrong random prior probability estimate
        System.out.println("==== Classification prior probability is completely random ====");
        for (int i = 0; i < repetitionCount; i++) {
            PriorProbability randomProbability = PriorProbability.randomize(random, new ArrayList<>(table.getGroupsNames()));
            PriorProbability randomEstimate = PriorProbability.randomize(random, new ArrayList<>(table.getGroupsNames()));
            classifyRandom(random, table.makeCopy(), randomProbability, randomEstimate);
        }

        // VI -- repeat with random intercept
        // TODO
    }

    private static void classifyRandom(Random random, ClassificationTable table,
                                       PriorProbability simulatedProbability,
                                       PriorProbability classificationProbability) {

        Classification.Builder<Set<String>> builder = new Classification.Builder<>();

        int keyCount = 100000; // TODO

        builder.setDataSetIterator(SimulatedDataSetIterator.fromClassificationTable(table.makeCopy(),
                simulatedProbability, keyCount, random));
        builder.setDataSetSaver(new NoActionDataSetSaver());
        builder.setTable(table.makeCopy());
        builder.setPropertyExtractor(new SourcePropertyExtractor());
        if (classificationProbability != null) {
            builder.setPriorProbabilityEstimator(new UserDefinedPriorProbabilityEstimator(classificationProbability));
        } else {
            builder.setPriorProbabilityEstimator(new NonNegativeLeastSquaresFitPriorProbabilityEstimator(table));
        }
        BatchesStatisticsAggregator aggregator = new BatchesStatisticsAggregator(new ArrayList<>(table.getGroupsNames()), null);
        builder.setStatisticsAggregator(aggregator);

        try {
            builder.build().classify();
        } catch (DataSetException e) {
            System.err.println("Classification of simulated dataset failed");
            e.printStackTrace(System.err);
        }

        if (classificationProbability == null) classificationProbability = aggregator.getPriorProbability();

        Map<Long, BatchStatistic> batches = aggregator.getPositiveUniqueBatches();
        // TODO modify once larger batches are simulated
        BatchStatistic statistic = batches.get(1L);
        if (statistic == null) {
            System.err.println("Classification of simulated dataset failed");
            return;
        }
        PriorProbability classificationResult = PriorProbability.fromMap(statistic.getCommonClassification().getValues());
        System.out.println("Distance to true prior:           " + simulatedProbability.distance(classificationResult));
        System.out.println("Distance to classification prior: " + classificationProbability.distance(classificationResult));
        System.out.println();
    }

    private static Map<String, BigDecimal> simulateStatistics(ClassificationTable table, PriorProbability probability) {
        Map<String, BigDecimal> maskToFrequency = new TreeMap<>();

        // TODO ensure table column-normalized

        List<String> masks = table.getMasks();

        for (String mask : masks) {
            ClassificationRow row = table.classifyIdentification(mask);
            ClassificationRow rowWithPrior = row.deepCopy();
            rowWithPrior.applyPriorProbabilities(probability, false);
            BigDecimal weighedSum = BigDecimal.ZERO;
            for (BigDecimal weighedProbability : rowWithPrior.getValues().values()) {
                weighedSum = weighedSum.add(weighedProbability);
            }
            maskToFrequency.put(mask, weighedSum);
        }

        return maskToFrequency;
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
