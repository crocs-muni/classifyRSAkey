package cz.crcs.sekan.rsakeysanalysis.classification.tests.util;

import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xnemec1
 * @version 3/15/17.
 */
public class DistributionsComparator {

    private List<Double> pValues;
    private DescriptiveStatistics statistics;

    public DistributionsComparator() {
        this.pValues = new ArrayList<>();
        this.statistics = new DescriptiveStatistics();
    }

    public double addDistributions(double[] expected, long[] observed) {
        double pValue = compareDistributions(expected, observed);
        pValues.add(pValue);
        statistics.addValue(pValue);
        return pValue;
    }

    public static boolean uniformityTest(double pValue, double confidence) {
        return pValue > 1d - confidence;
    }

    public String conclusion(double confidence) {
        double pValue = equalityPValue();
        String result = "The p-value of uniformity test is %f. The observed distributions %s conform to the expected distributions (confidence interval %f).";
        return String.format(result, pValue, uniformityTest(pValue, confidence) ? "may" : "do not", confidence);
    }

    public static String percentilesHeader(int step) {
        StringBuilder builder = new StringBuilder();
        for (int percentile = step; percentile <= 100; percentile += step) {
            builder.append(String.format("%d;", percentile));
        }
        return builder.toString();
    }

    public String getPercentiles(int step) {
        StringBuilder builder = new StringBuilder();
        for (int percentile = step; percentile <= 100; percentile += step) {
            builder.append(String.format("%1.3f;", statistics.getPercentile(percentile)));
        }
        return builder.toString();
    }

    public double equalityPValue() {
        KolmogorovSmirnovTest uniformityTest = new KolmogorovSmirnovTest();
        double uniformityPValue = uniformityTest.kolmogorovSmirnovTest(new UniformRealDistribution(), pValues.stream().mapToDouble(Double::doubleValue).toArray());
        return uniformityPValue;
    }

    public static double compareDistributions(double[] expected, long[] observed) {
        return compareDistributions(expected, observed, false);
    }

    public static List<Double> allDifferences(double[] expected, double[] observed) {
        if (expected.length != observed.length) {
            System.err.println("compareDistributions: incorrect lengths");
            return null;
        }

        List<Double> differences = new ArrayList<>(expected.length);

        for (int i = 0; i < expected.length; i++) {
            double difference = Math.abs(expected[i] - observed[i]);
            differences.add(difference);
        }

        return differences;
    }

    public static double largestDifference(double[] expected, double[] observed) {
        if (expected.length != observed.length) {
            System.err.println("compareDistributions: incorrect lengths");
            return 1d;
        }

        double largestDifference = 0d;

        for (int i = 0; i < expected.length; i++) {
            double difference = Math.abs(expected[i] - observed[i]);
            if (difference > largestDifference) {
                largestDifference = difference;
            }
        }

        return largestDifference;
    }

    public static double compareDistributions(double[] expected, long[] observed, boolean preciseMatch) {
        if (expected.length != observed.length) {
            System.err.println("compareDistributions: incorrect lengths");
            return 0d;
        }

        // the ChiSquareTest can't handle expected values of 0 (even if observed was also 0)
        boolean expectedZeroes = false;
        int nonZero = 0;
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] <= 0d) {
                if (observed[i] > 0d) return 0; // 0 was expected but some were observed -> definitely different
                expectedZeroes = true;
            } else {
                nonZero++;
            }
        }

        ChiSquareTest chiSquareTest = new ChiSquareTest();

        if (expectedZeroes) {
            double[] expectedNoZeroes = new double[nonZero];
            long[] observedNoZeroes = new long[nonZero];
            int newIndex = 0;
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] > 0d) {
                    expectedNoZeroes[newIndex] = expected[i];
                    observedNoZeroes[newIndex] = observed[i];
                    newIndex++;
                }
            }
            expected = expectedNoZeroes;
            observed = observedNoZeroes;
        }

        if (!preciseMatch) {
            return chiSquareTest.chiSquareTest(expected, observed);
        }

        long sampleSize = 0L;
        for (long samples : observed) {
            sampleSize += samples;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((long) (expected[i] * sampleSize) != observed[i]) {
                return 0d;
            }
        }
        return 1d;
    }

}
