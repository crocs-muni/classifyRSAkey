package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;

/**
 * @author xnemec1
 * @version 11/24/16.
 */
public class UserDefinedPriorProbabilityEstimator extends PriorProbabilityEstimator {

    private PriorProbability priorProbability;

    public UserDefinedPriorProbabilityEstimator(ClassificationTable table) {
        super(table);
        priorProbability = table.getPriorProbability();
    }

    public UserDefinedPriorProbabilityEstimator(PriorProbability priorProbability) {
        this.priorProbability = priorProbability;
    }

    @Override
    public void addMask(String mask) {
        // no need to compute anything
    }

    @Override
    public PriorProbability computePriorProbability() {
        return priorProbability;
    }
}
