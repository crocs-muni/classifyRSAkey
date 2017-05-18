package cz.crcs.sekan.rsakeysanalysis.classification.tests;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.NonNegativeLeastSquaresFitPriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbability;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author xnemec1
 * @version 5/18/17.
 */
public class Misclassification {

    public static void compute(ClassificationTable originalTable) {
        List<String> groupNames = new ArrayList<>(originalTable.getGroupsNames());

        JSONObject json = new JSONObject();
        for (String group : groupNames) {
            ClassificationTable table = originalTable.makeCopy();
            Map<String, BigDecimal> maskToCountOfRemovedGroup = table.removeGroup(group);

            PriorProbabilityEstimator estimator = new NonNegativeLeastSquaresFitPriorProbabilityEstimator(table.makeCopy());
            estimator.setMaskToFrequency(maskToCountOfRemovedGroup);
            PriorProbability priorProbability = estimator.computePriorProbability();

            json.put(group, priorProbability.normalized().toJSON());

            System.out.println(group);
            System.out.println(priorProbability.normalized().toJSON());
        }
        System.out.println();
        System.out.println(json);
    }

}
