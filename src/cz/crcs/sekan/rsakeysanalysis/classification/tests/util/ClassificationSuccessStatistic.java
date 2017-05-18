package cz.crcs.sekan.rsakeysanalysis.classification.tests.util;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbability;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics.BatchStatistic;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author xnemec1
 * @version 2/28/17.
 */
public class ClassificationSuccessStatistic {
    private Map<Integer, BigDecimal> guessOrderToCount;

    public ClassificationSuccessStatistic() {
        this.guessOrderToCount = new TreeMap<>();
    }

    public ClassificationSuccessStatistic(Map<Integer, BigDecimal> guessOrderToCount) {
        this.guessOrderToCount = guessOrderToCount;
    }

    public BigDecimal addGuessWithOrder(Integer guessOrder) {
        return guessOrderToCount.compute(guessOrder, (key, oldValue) -> oldValue == null ? BigDecimal.ONE : oldValue.add(BigDecimal.ONE));
    }

    public List<BigDecimal> toProbability(Integer maxGuesses, boolean neverGuessedIsMax) {
        if (!neverGuessedIsMax) {
            maxGuesses++;
        }
        List<BigDecimal> probabilities = new ArrayList<>(maxGuesses);
        BigDecimal sum = BigDecimal.ZERO;
        for (int guesses = 0; guesses < maxGuesses; guesses++) {
            BigDecimal count = guessOrderToCount.getOrDefault(guesses + 1 == maxGuesses ? -1 : guesses, BigDecimal.ZERO);
            probabilities.add(count);
            sum = sum.add(count);
        }
        if (BigDecimal.ZERO.compareTo(sum) != 0) {
            BigDecimal finalSum = sum;
            probabilities.replaceAll((x) -> x.divide(finalSum, PriorProbability.BIG_DECIMAL_SCALE, BigDecimal.ROUND_HALF_EVEN));
        }
        return probabilities;
    }

    public List<BigDecimal> toCumulativeDistribution(Integer maxGuesses, boolean neverGuessedIsMax) {
        List<BigDecimal> probabilities = toProbability(maxGuesses, neverGuessedIsMax);
        List<BigDecimal> cumulativeDistribution = new ArrayList<>(probabilities.size());
        BigDecimal cumulative = BigDecimal.ZERO;
        for (BigDecimal probability : probabilities) {
            cumulative = cumulative.add(probability);
            cumulativeDistribution.add(cumulative);
        }
        return cumulativeDistribution;
    }

    public static String rowStatisticHeader(Long seed, List<Integer> guessCounts, String separator) {
        StringBuilder builder = new StringBuilder();
        builder.append("Seed: ");
        builder.append(seed);
        builder.append(separator);
        int counter = guessCounts.size();
        for (Integer count : guessCounts) {
            builder.append(count);
            if (--counter > 0) builder.append(separator);
        }
        return builder.toString();
    }

    public static String toRowStatistic(String groupName, List<BigDecimal> probabilities, DecimalFormat formatter, String separator) {
        StringBuilder builder = new StringBuilder();
        builder.append(groupName);
        builder.append(separator);
        int counter = probabilities.size();
        for (BigDecimal count : probabilities) {
            builder.append(formatter.format(count));
            if (--counter > 0) builder.append(separator);
        }
        return builder.toString();
    }

    public static ClassificationSuccessStatistic weighedAverage(Map<String, ClassificationSuccessStatistic> groupNameToStatistic, PriorProbability weighs, Integer maxGuesses, boolean neverGuessedIsMax) {
        if (!weighs.keySet().containsAll(groupNameToStatistic.keySet())) {
            System.err.println(weighs.keySet());
            System.err.println(groupNameToStatistic.keySet());
            throw new IllegalArgumentException("Different groups");
        }
        PriorProbability normalizedWeighs = weighs.normalized();
        List<String> groupNames = new ArrayList<>(groupNameToStatistic.keySet());
        ClassificationSuccessStatistic average = new ClassificationSuccessStatistic();

        for (int guesses = 0; guesses < maxGuesses; guesses++) {
            int index = guesses + 1 == maxGuesses ? -1 : guesses;
            BigDecimal sum = BigDecimal.ZERO;
            for (String group : groupNames) {
                 sum = sum.add(groupNameToStatistic.get(group).guessOrderToCount.getOrDefault(index, BigDecimal.ZERO).multiply(normalizedWeighs.get(group)));
            }
            average.guessOrderToCount.put(index, sum);
        }
        return average;
    }
}
