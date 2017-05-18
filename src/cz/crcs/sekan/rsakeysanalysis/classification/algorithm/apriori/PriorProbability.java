package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori;

import cz.crcs.sekan.rsakeysanalysis.classification.tests.util.ClassificationSuccessStatisticsAggregator;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.json.simple.JSONObject;
import sun.awt.AWTAccessor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Vector of probabilities, summing to one, unless scale using scale().
 *
 * BigDecimal is used for probabilities, because we often encounter batches of several keys. That leads to high powers
 * of the probabilities and loss of precision with ordinary types (e.g. 11 bits of exponent in double are not enough
 * and we would lose precision on mantissa)
 *
 * @author xnemec1
 * @version 11/24/16.
 */
public class PriorProbability extends TreeMap<String, BigDecimal> {

    public static final double ERROR_MEASURE_NOT_AVAILABLE = Double.NaN;

    public enum Distribution {
        EVEN,
        UNIFORM,
        GEOMETRIC,
        CUSTOM
    }

    private double errorMeasure;

    private double distributionFitPValue;

    public PriorProbability() {
        errorMeasure = ERROR_MEASURE_NOT_AVAILABLE;
    }

    public double getErrorMeasure() {
        return errorMeasure;
    }

    public void setErrorMeasure(double errorMeasure) {
        this.errorMeasure = errorMeasure;
    }

    public double getDistributionFitPValue() {
        return distributionFitPValue;
    }

    public void setDistributionFitPValue(double distributionFitPValue) {
        this.distributionFitPValue = distributionFitPValue;
    }

    public BigDecimal setGroupProbability(String groupName, BigDecimal priorProbability) {
        return put(groupName, priorProbability);
    }

    public BigDecimal getGroupProbability(String groupName) {
        return get(groupName);
    }

    public static PriorProbability fromMap(Map<String, BigDecimal> map) {
        PriorProbability probability = new PriorProbability();
        probability.putAll(map);
        return probability;
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        List<String> sortedGroups = new ArrayList<>(keySet());
        Collections.sort(sortedGroups);
        for (String groupName : sortedGroups) {
            object.put(groupName, getGroupProbability(groupName).toString());
        }
        return object;
    }

    public String toCSV(List<String> groupNames, String separator) {
        StringBuilder builder = new StringBuilder();
        DecimalFormat formatter = ClassificationSuccessStatisticsAggregator.defaultFormatter();
        int groupsLeft = groupNames.size();
        for (String group : groupNames) {
            builder.append(formatter.format(getOrDefault(group, BigDecimal.ZERO)));
            if (--groupsLeft > 0) builder.append(separator);
        }
        return builder.toString();
    }

    public static final int BIG_DECIMAL_SCALE = 20;

    public static PriorProbability uniformProbability(List<String> groupNames) {
        PriorProbability priorProbability = new PriorProbability();
        BigDecimal probability = BigDecimal.ONE.divide(BigDecimal.valueOf(groupNames.size()), BIG_DECIMAL_SCALE, BigDecimal.ROUND_HALF_EVEN);
        for (String groupName : groupNames) {
            priorProbability.put(groupName, probability);
        }
        return priorProbability;
    }

    public PriorProbability makeCopy() {
        PriorProbability copy = new PriorProbability();
        for (Map.Entry<String, BigDecimal> entry : entrySet()) {
            copy.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    public PriorProbability normalized() {
        BigDecimal sum = values().stream().reduce(BigDecimal::add).orElseGet(() -> BigDecimal.ONE);
        if (BigDecimal.ZERO.equals(sum)) {
            return makeCopy();
        }
        PriorProbability normalized = new PriorProbability();
        for (Map.Entry<String, BigDecimal> entry : entrySet()) {
            normalized.put(entry.getKey(), entry.getValue().divide(sum, BIG_DECIMAL_SCALE, BigDecimal.ROUND_HALF_EVEN));
        }
        return normalized;
    }

    public static PriorProbability randomize(Random random, List<String> groupNames) {
        PriorProbability priorProbability = new PriorProbability();
        for (String groupName : groupNames) {
            priorProbability.put(groupName, BigDecimal.valueOf(random.nextDouble()));
        }
        return priorProbability.normalized();
    }

    public static PriorProbability randomizeGeometric(Random random, List<String> groupNames, BigDecimal probability, BigDecimal tailProbability) {
        PriorProbability priorProbability = new PriorProbability();
        BigDecimal sourceProbability = probability.abs();
        List<String> unusedGroupNames = new ArrayList<>(groupNames);
        BigDecimal remainingProbability = BigDecimal.ONE.subtract(sourceProbability);
        while (!unusedGroupNames.isEmpty()) {
            int nextGroup = random.nextInt(unusedGroupNames.size());
            priorProbability.put(unusedGroupNames.get(nextGroup), sourceProbability);
            if (tailProbability.compareTo(remainingProbability.multiply(probability)) > 0) {
                sourceProbability = tailProbability;
            } else {
                sourceProbability = remainingProbability.multiply(probability);
                remainingProbability = remainingProbability.subtract(sourceProbability);
            }
            unusedGroupNames.remove(nextGroup);
        }
        return priorProbability.normalized();
    }

    public static PriorProbability randomizeFromPrior(Random random, PriorProbability referenceProbability, BigDecimal noise) {
        PriorProbability noisyProbability = referenceProbability.makeCopy();
        for (String group : noisyProbability.keySet()) {
            noisyProbability.setGroupProbability(group, noisyProbability.getGroupProbability(group).add(
                    noise.multiply(BigDecimal.valueOf(2)).multiply(BigDecimal.valueOf(random.nextFloat())))
                    .subtract(noise.divide(BigDecimal.ONE.subtract(noise), BigDecimal.ROUND_HALF_EVEN)).abs());
        }
        return noisyProbability.normalized();
    }

    public PriorProbability scale(BigDecimal scale) {
        PriorProbability scaled = new PriorProbability();
        for (Map.Entry<String, BigDecimal> entry : entrySet()) {
            scaled.put(entry.getKey(), entry.getValue().multiply(scale));
        }
        return scaled;
    }

    public PriorProbability sum(PriorProbability other) {
        PriorProbability summed = new PriorProbability();
        if (!other.keySet().containsAll(keySet()) || !keySet().containsAll(other.keySet())) {
            throw new IllegalArgumentException("Probability contain different sources");
        }
        for (Map.Entry<String, BigDecimal> entry : entrySet()) {
            summed.put(entry.getKey(), entry.getValue().add(other.getOrDefault(entry.getKey(), BigDecimal.ZERO)));
        }
        return summed;
    }

    public double[] toDoubleArray(List<String> groupsOrdered) {
        if (!keySet().containsAll(groupsOrdered)) {
            System.err.println("toDoubleArray: Some requested groups are missing");
            return null;
        }
        double[] probabilities = new double[groupsOrdered.size()];
        int i = 0;
        for (String groupName : groupsOrdered) {
            probabilities[i++] = getOrDefault(groupName, BigDecimal.ZERO).doubleValue();
        }
        return probabilities;
    }

    public long[] toLongArray(List<String> groupsOrdered, long sampleSize) {
        double[] doubleArray = toDoubleArray(groupsOrdered);
        long[] longArray = new long[doubleArray.length];
        for (int i = 0; i < doubleArray.length; i++) {
            longArray[i] = (long) (doubleArray[i] * sampleSize);
        }
        return longArray;
    }

    public List<BigDecimal> toBigDecimalList(List<String> groupsOrdered) {
        if (!keySet().containsAll(groupsOrdered)) {
            System.err.println("toBigDecimalList: Some requested groups are missing");
            return null;
        }
        List<BigDecimal> probabilities = new ArrayList<>(groupsOrdered.size());
        for (String groupName : groupsOrdered) {
            probabilities.add(getOrDefault(groupName, BigDecimal.ZERO));
        }
        return probabilities;
    }

    public double distance(PriorProbability other, long sampleSize) {
        if (other.keySet().size() != this.keySet().size() || !other.keySet().containsAll(this.keySet())) {
            System.err.println("The prior probabilities do not contain equal groups");
            return 0d;
        }
        List<String> groupNames = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : entrySet()) {
            if (entry.getValue().doubleValue() > 0d) {
                groupNames.add(entry.getKey());
            } else {
                BigDecimal otherProbability = other.getGroupProbability(entry.getKey());
                if (otherProbability != null && otherProbability.doubleValue() > 0d) {
                    // value is not expected, but it is present
                    return 0d;
                }
            }
        }
        ChiSquareTest test = new ChiSquareTest();
        double[] observed = other.normalized().toDoubleArray(groupNames);
        long[] observedCounts = new long[observed.length];
        for (int i = 0; i < observed.length; i++) {
            observedCounts[i] = Double.valueOf(observed[i]*sampleSize).longValue();
        }
        double pValue = test.chiSquareTest(this.toDoubleArray(groupNames), observedCounts);
        return pValue;
    }

    public BigDecimal distanceLegacy(PriorProbability other) {
        BigDecimal distance = BigDecimal.ZERO;
        if (other.keySet().size() != this.keySet().size() || !other.keySet().containsAll(this.keySet())) {
            System.err.println("distance: The prior probabilities do not contain equal groups");
            return null;
        }
        PriorProbability otherNormalized = other.normalized();
        PriorProbability thisNormalized = normalized();
        for (String group : thisNormalized.keySet()) {
            BigDecimal divisor = thisNormalized.get(group);
            if (BigDecimal.ZERO.compareTo(divisor) > -1) {
                divisor = BigDecimal.ONE.divide(BigDecimal.valueOf(10000), BIG_DECIMAL_SCALE, BigDecimal.ROUND_HALF_EVEN);
            }
            distance = distance.add(((otherNormalized.get(group).subtract(thisNormalized.get(group)))
                    .divide(divisor, BIG_DECIMAL_SCALE, BigDecimal.ROUND_HALF_EVEN)).abs());
        }
        return distance;
    }
}
