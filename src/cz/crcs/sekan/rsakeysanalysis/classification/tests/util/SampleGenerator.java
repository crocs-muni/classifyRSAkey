package cz.crcs.sekan.rsakeysanalysis.classification.tests.util;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbability;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author xnemec1
 * @version 2/28/17.
 */
public class SampleGenerator<SampleType> {
    private List<SampleType> samples;
    private List<BigDecimal> cumulativeDistribution;

    public SampleGenerator(List<SampleType> samples, List<BigDecimal> probabilities) {
        if (samples.size() != probabilities.size()) {
            throw new IllegalArgumentException("The number of samples and probabilities does not match");
        }
        this.samples = samples;
        this.cumulativeDistribution = new ArrayList<>(probabilities.size());

        BigDecimal cumulative = BigDecimal.ZERO;
        for (BigDecimal probability : probabilities) {
            cumulative = cumulative.add(probability);
            this.cumulativeDistribution.add(cumulative);
        }
        if (BigDecimal.ONE.compareTo(cumulative) != 0 && BigDecimal.ZERO.compareTo(cumulative) != 0) {
//            throw new IllegalArgumentException("The probabilities do not sum up to one, but to " + cumulative);
            BigDecimal finalCumulative = cumulative;
            cumulativeDistribution.replaceAll((x) -> x.divide(finalCumulative, PriorProbability.BIG_DECIMAL_SCALE, BigDecimal.ROUND_HALF_EVEN));
        }
    }

    public SampleType getRandomSample(BigDecimal random) {
        if (BigDecimal.ZERO.compareTo(random) == 1 || BigDecimal.ONE.compareTo(random) == -1) {
            throw new IllegalArgumentException("Random number must be from 0 to 1");
        }
        int index = Collections.binarySearch(cumulativeDistribution, random);
        if (index < 0) {
            index = -(index + 1);
        }
        if (index >= samples.size()) {
            index = samples.size() - 1;
        }
        return samples.get(index);
    }
}
