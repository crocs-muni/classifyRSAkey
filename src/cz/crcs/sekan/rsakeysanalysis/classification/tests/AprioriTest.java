package cz.crcs.sekan.rsakeysanalysis.classification.tests;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.ClassificationConfiguration;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.NonNegativeLeastSquaresFitPriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbability;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * @author xnemec1
 * @version 3/2/17.
 */
public class AprioriTest {

    private static final String INTERCEPT_GROUP_NAME = "Intercept";
    private static final double MAX_INTERCEPT_PROBABILITY = 0.05;

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
