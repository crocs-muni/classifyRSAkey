package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;

/**
 * @author xnemec1
 * @version 11/24/16.
 */
public interface StatisticsAggregator {
    public void addStatistics(ClassificationContainer container);

    public void saveStatistics();

    public void savePriorProbabilitySummary(PriorProbabilityEstimator estimator);
}
