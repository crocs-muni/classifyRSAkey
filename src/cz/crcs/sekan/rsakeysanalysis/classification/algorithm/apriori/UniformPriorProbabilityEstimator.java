package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author xnemec1
 * @version 11/24/16.
 */
public class UniformPriorProbabilityEstimator extends PriorProbabilityEstimator {
    public UniformPriorProbabilityEstimator(ClassificationTable table) {
        super(table);
    }

    @Override
    public void addMask(String mask) {
        // no need to compute anything
    }

    @Override
    public PriorProbability computePriorProbability() {
        Set<String> groupNames = table.getGroupsNames();
        BigDecimal uniformProbability = BigDecimal.ONE;

        PriorProbability probabilities = new PriorProbability();

        for (String group : groupNames) {
            probabilities.put(group, uniformProbability);
        }

        return probabilities;
    }
}
