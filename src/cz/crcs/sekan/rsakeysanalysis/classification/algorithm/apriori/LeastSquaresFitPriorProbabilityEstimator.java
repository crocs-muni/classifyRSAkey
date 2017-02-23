package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import org.apache.commons.math3.fitting.leastsquares.*;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Deprecated - not working
 *
 * @author xnemec1
 * @version 11/24/16.
 */
@Deprecated
public class LeastSquaresFitPriorProbabilityEstimator extends PriorProbabilityEstimator {

    private List<String> allMaskValues;
    private RealVector observedFrequencies;
    private RealMatrix maskProbabilitiesForSources;

    public LeastSquaresFitPriorProbabilityEstimator(ClassificationTable table) {
        super(table);

    }

    private static RealVector normalizeVector(RealVector vector) {
        double sum = 0d;
        for (double d : vector.toArray()) {
            sum += d;
        }
        return vector.mapDivide(sum);
    }

    private static RealVector positifyVector(RealVector vector) {
        for (int i = 0; i < vector.getDimension(); i++) {
            double entry = vector.getEntry(i);
            if (entry < 0) vector.setEntry(i, -entry);
        }
        return vector;
    }

    private static RealVector zeroNegativeElements(RealVector vector) {
        for (int i = 0; i < vector.getDimension(); i++) {
            double entry = vector.getEntry(i);
            if (entry < 0) vector.setEntry(i, 0);
        }
        return vector;
    }

    private MultivariateJacobianFunction distanceToRealDataset = new MultivariateJacobianFunction() {
        public Pair<RealVector, RealMatrix> value(final RealVector aprioriApproximation) {

            int maskCount = allMaskValues.size();
            int groupCount = table.getGroupsNames().size();

            RealMatrix aprioriApproximationMatrix = new Array2DRowRealMatrix(aprioriApproximation.getDimension(), 1);
            aprioriApproximationMatrix.setColumn(0, aprioriApproximation.toArray());

            RealVector approximation = maskProbabilitiesForSources.multiply(aprioriApproximationMatrix).getColumnVector(0);
            approximation = normalizeVector(approximation);
            RealVector differences = observedFrequencies.subtract(approximation);

            RealMatrix jacobian = new Array2DRowRealMatrix(maskCount, groupCount);
            for (int i = 0; i < groupCount; i++) {
                RealVector partialDifferences = observedFrequencies.subtract(maskProbabilitiesForSources.getColumnVector(i).mapMultiply(aprioriApproximation.getEntry(i)));
                jacobian.setColumn(i, partialDifferences.toArray());//.ebeDivide(differences).toArray());
                //if (i == 0) System.out.println(partialDifferences);
            }

            return new Pair<>(differences, jacobian);
        }
    };

    @Override
    public PriorProbability computePriorProbability() {
        List<String> groupNames = new ArrayList<>(table.getGroupsNames());
        int groupCount = groupNames.size();

        allMaskValues = new ArrayList<>(table.getTable().keySet());

        this.observedFrequencies = new ArrayRealVector(allMaskValues.size());
        for (int i = 0; i < allMaskValues.size(); i++) {
            Long maskFrequency = maskToFrequency.get(allMaskValues.get(i));
            if (maskFrequency == null) maskFrequency = 0L;
            this.observedFrequencies.setEntry(i, maskFrequency.doubleValue());
        }

        observedFrequencies = normalizeVector(observedFrequencies);
        // TODO normalize

        maskProbabilitiesForSources = new Array2DRowRealMatrix(allMaskValues.size(), groupCount);

        for (int i = 0; i < allMaskValues.size(); i++) {
            ClassificationRow row = table.getTable().get(allMaskValues.get(i));
            // TODO NO row.normalize();

            for (String groupName : table.getGroupsNames()) {
                int groupIndex = groupNames.indexOf(groupName);
                BigDecimal maskGroupProbability = row.getValues().get(groupName);
                if (maskGroupProbability == null) {
                    // impossible mask
                    maskGroupProbability = BigDecimal.ZERO;
                }
                maskProbabilitiesForSources.setEntry(i, groupIndex, maskGroupProbability.doubleValue());
            }
        }

        double[] startingPoints = new double[groupCount];
        for (int i = 0; i < groupCount; i++) startingPoints[i] = 1d / groupCount;

        double[] targetDistances = new double[allMaskValues.size()];
        for (int i = 0; i < allMaskValues.size(); i++) targetDistances[i] = 0d;

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .start(startingPoints)
                .model(distanceToRealDataset)
                .target(targetDistances)
                .parameterValidator(LeastSquaresFitPriorProbabilityEstimator::zeroNegativeElements)//realVector -> normalizeVector(positifyVector(realVector)))//LeastSquaresFitPriorProbabilityEstimator::normalizeVector)
                .maxEvaluations(10000)
                .maxIterations(10000)
                .build();

        LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer().
                withCostRelativeTolerance(1.0e-12).
                withParameterRelativeTolerance(1.0e-12);

        //optimizer = new GaussNewtonOptimizer();//.withDecomposition(GaussNewtonOptimizer.Decomposition.QR);

        LeastSquaresOptimizer.Optimum optimum = optimizer.optimize(problem);

        if (optimum.getPoint().getDimension() != groupCount) {
            throw new IllegalStateException("Incorrect solution size");
        }

        //System.out.println(optimum.getResiduals());
        System.out.println(optimum.getPoint());

        PriorProbability priorProbability = new PriorProbability();
        for (int i = 0; i < groupCount; i++) {
            priorProbability.put(groupNames.get(i), BigDecimal.valueOf(optimum.getPoint().getEntry(i)));
        }
        return priorProbability;
    }
}
