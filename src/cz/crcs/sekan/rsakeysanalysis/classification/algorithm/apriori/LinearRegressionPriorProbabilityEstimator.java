package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xnemec1
 * @version 12/7/16.
 */
@Deprecated
public class LinearRegressionPriorProbabilityEstimator extends PriorProbabilityEstimator {
    public LinearRegressionPriorProbabilityEstimator(ClassificationTable table) {
        super(table);
    }

    private static double[] normalize(double[] array) {
        double sum = 0d;
        for (double a : array) {
            sum += a;
        }
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] / sum;
        }
        return result;
    }

    @Override
    public PriorProbability computePriorProbability() {
        OLSMultipleLinearRegression multipleLinearRegression = new OLSMultipleLinearRegression();
        multipleLinearRegression.setNoIntercept(true); // no intercept -- constant value

        List<String> groupNames = new ArrayList<>(table.getGroupsNames());
        int groupCount = groupNames.size();
        List<String> allMaskValues = new ArrayList<>(table.getTable().keySet());
        int maskCount = allMaskValues.size();

        double[] observedFrequencies = new double[maskCount];
        double[][] libraryFrequencies = new double[maskCount][groupCount];

        for (int i = 0; i < allMaskValues.size(); i++) {
            Long maskFrequency = maskToFrequency.get(allMaskValues.get(i));
            if (maskFrequency == null) maskFrequency = 0L;
            observedFrequencies[i] = maskFrequency.doubleValue();
        }
        observedFrequencies = normalize(observedFrequencies);

        for (int i = 0; i < allMaskValues.size(); i++) {
            ClassificationRow row = table.getTable().get(allMaskValues.get(i));
            // remember, NO row.normalize();

            for (int j = 0; j < groupNames.size(); j++) {
                BigDecimal maskGroupProbability = row.getValues().get(groupNames.get(j));
                if (maskGroupProbability == null) {
                    // impossible mask
                    maskGroupProbability = BigDecimal.ZERO;
                }
                libraryFrequencies[i][j] = maskGroupProbability.doubleValue();
            }
        }

        for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
            double sum = 0d;
            for (int i = 0; i < maskCount; i++) {
                sum += libraryFrequencies[i][groupIndex];
            }
            for (int i = 0; i < maskCount; i++) {
                libraryFrequencies[i][groupIndex] /= sum;
            }
        }

        multipleLinearRegression.newSampleData(observedFrequencies, libraryFrequencies);

        double[] parameters = multipleLinearRegression.estimateRegressionParameters();
        parameters = normalize(parameters);

        for (double parameter : parameters) {
            System.out.println(parameter);
        }

        PriorProbability priorProbability = new PriorProbability();
        for (int i = 0; i < groupCount; i++) {
            priorProbability.put(groupNames.get(i), BigDecimal.valueOf(parameters[i]));
        }

        return priorProbability;
    }
}
