package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori;

import cz.crcs.sekan.rsakeysanalysis.classification.tests.util.ClassificationSuccessStatisticsAggregator;
import org.json.simple.JSONObject;
import sun.awt.AWTAccessor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BinaryOperator;

/**
 * @author xnemec1
 * @version 11/24/16.
 */
public class PriorProbability extends TreeMap<String, BigDecimal> {
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

    public BigDecimal distance(PriorProbability other) {
        BigDecimal distance = BigDecimal.ZERO;
        if (!other.keySet().containsAll(this.keySet())) {
            System.err.println("The prior probabilities do not contain equal groups");
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
