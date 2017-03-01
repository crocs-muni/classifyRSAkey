package cz.crcs.sekan.rsakeysanalysis.classification.tests.util;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbability;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset.DataSetIterator;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Dataset simulator. Generates random keys on the fly.
 *
 * @author xnemec1
 * @version 2/27/17.
 */
public class SimulatedDataSetIterator implements DataSetIterator {
    private int simulatedKeyCount;
    private int lastKeyId;
    private Random random;
    private SampleGenerator<ClassificationGroupSimulator> priorProbabilityGenerator;

    public static SimulatedDataSetIterator fromClassificationTable(ClassificationTable table, PriorProbability priorProbability,
                                                                   int maxKeyCount, Random random) {
        List<String> groupNames = new ArrayList<>(table.getGroupsNames());
        List<ClassificationGroupSimulator> groups = new ArrayList<>(groupNames.size());
        List<BigDecimal> groupProbabilities = new ArrayList<>(groupNames.size());

        for (String groupName : groupNames) {
            groups.add(ClassificationGroupSimulator.fromClassificationTable(table, groupName));
            groupProbabilities.add(priorProbability.getGroupProbability(groupName));
        }

        SimulatedDataSetIterator simulator = new SimulatedDataSetIterator();
        simulator.lastKeyId = 0;
        simulator.simulatedKeyCount = maxKeyCount;
        simulator.random = random;
        simulator.priorProbabilityGenerator = new SampleGenerator<>(groups, groupProbabilities);
        return simulator;
    }


    @Override
    public void close() {
        // nothing
    }

    @Override
    public boolean hasNext() {
        return lastKeyId < simulatedKeyCount;
    }

    @Override
    public ClassificationKey next() {
        RSAKey rsaKey = new RSAKey();
        rsaKey.setModulus(BigInteger.valueOf(lastKeyId));

        Set<String> source = null; // TODO in case that batching is tested
        int count = 1; // TODO in case that duplicity count is tested
        ClassificationGroupSimulator randomGroup = randomGroup();
        String mask = randomGroup.getMaskFromRandom(uniformRandom(random));

        lastKeyId++;

        return new ClassificationKey(rsaKey, source, count, mask, randomGroup.getGroupName());
    }

    private static BigDecimal uniformRandom(Random random) {
        Double rand = random.nextDouble();
        return BigDecimal.valueOf(rand);
    }

    private ClassificationGroupSimulator randomGroup() {
        return priorProbabilityGenerator.getRandomSample(uniformRandom(random));
    }
}
