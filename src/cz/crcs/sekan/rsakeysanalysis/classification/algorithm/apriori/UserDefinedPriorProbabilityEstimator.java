package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author xnemec1
 * @version 11/24/16.
 */
public class UserDefinedPriorProbabilityEstimator extends PriorProbabilityEstimator {
    public UserDefinedPriorProbabilityEstimator(ClassificationTable table) {
        super(table);
    }

    @Override
    public void addMask(String mask) {
        // no need to compute anything
    }

    @Override
    public PriorProbability computePriorProbability() {
        return table.getPriorProbability();
    }
}
