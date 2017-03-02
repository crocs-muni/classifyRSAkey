package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import edu.rit.numeric.NonNegativeLeastSquares;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xnemec1
 * @version 2/22/17.
 */
public class NonNegativeLeastSquaresFitPriorProbabilityEstimator extends PriorProbabilityEstimator {
    public NonNegativeLeastSquaresFitPriorProbabilityEstimator(ClassificationTable table) {
        super(table);
    }

    @Override
    public PriorProbability computePriorProbability() {
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

        // normalize column-wise (group-wise)
        for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
            double sum = 0d;
            for (int i = 0; i < maskCount; i++) {
                sum += libraryFrequencies[i][groupIndex];
            }
            for (int i = 0; i < maskCount; i++) {
                libraryFrequencies[i][groupIndex] /= sum;
            }
        }

        NonNegativeLeastSquares nnls = new NonNegativeLeastSquares(maskCount, groupCount);

        for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
            for (int i = 0; i < maskCount; i++) {
                nnls.a[i][groupIndex] = libraryFrequencies[i][groupIndex];
            }
        }
        System.arraycopy(observedFrequencies, 0, nnls.b, 0, maskCount);

        nnls.solve();

        double[] parameters = new double[groupCount];
        System.arraycopy(nnls.x, 0, parameters, 0, groupCount);

        parameters = normalize(parameters);

        PriorProbability priorProbability = new PriorProbability();
        for (int i = 0; i < groupCount; i++) {
            priorProbability.put(groupNames.get(i), BigDecimal.valueOf(parameters[i]));
        }

        return priorProbability;
    }

    private static double[] normalize(double[] array) {
        double sum = 0d;
        for (double a : array) {
            sum += a;
        }
        if (sum == 0) return array;
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] / sum;
        }
        return result;
    }
}
