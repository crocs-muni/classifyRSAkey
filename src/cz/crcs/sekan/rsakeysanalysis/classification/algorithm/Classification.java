package cz.crcs.sekan.rsakeysanalysis.classification.algorithm;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbability;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbabilityEstimator;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset.DataSetIterator;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset.DataSetSaver;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics.StatisticsAggregator;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKeyStub;
import cz.crcs.sekan.rsakeysanalysis.classification.key.property.PropertyExtractor;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xnemec1
 * @version 11/23/16.
 */
public class Classification<BatchProperty> {

    private PropertyExtractor<BatchProperty> propertyExtractor;

    private DataSetIterator dataSetIterator;

    private ClassificationTable table;

    private PriorProbabilityEstimator priorProbabilityEstimator;

    private StatisticsAggregator statisticsAggregator;

    private DataSetSaver dataSetSaver;


    private Map<Long, ClassificationKeyStub> keyIdToKeyStub;

    private BatchHolder<BatchProperty> batchHolder;

    private Classification() {
    }


    public static class Builder<BatchProperty> {
        private Classification<BatchProperty> classification;

        public Builder() {
            classification = new Classification<>();
        }

        public Builder<BatchProperty> setPropertyExtractor(PropertyExtractor<BatchProperty> propertyExtractor) {
            classification.propertyExtractor = propertyExtractor;
            return this;
        }

        public Builder<BatchProperty> setDataSetIterator(DataSetIterator dataSetIterator) {
            classification.dataSetIterator = dataSetIterator;
            return this;
        }

        public Builder<BatchProperty> setTable(ClassificationTable table) {
            classification.table = table;
            return this;
        }

        public Builder<BatchProperty> setPriorProbabilityEstimator(PriorProbabilityEstimator priorProbabilityEstimator) {
            classification.priorProbabilityEstimator = priorProbabilityEstimator;
            return this;
        }

        public Builder<BatchProperty> setStatisticsAggregator(StatisticsAggregator statisticsAggregator) {
            classification.statisticsAggregator = statisticsAggregator;
            return this;
        }

        public Builder<BatchProperty> setDataSetSaver(DataSetSaver dataSetSaver) {
            classification.dataSetSaver = dataSetSaver;
            return this;
        }

        public Classification<BatchProperty> build() {
            classification.keyIdToKeyStub = new TreeMap<>();
            classification.batchHolder = new BatchHolder<>(classification.propertyExtractor);
            return classification;
        }
    }

    public void classify() throws DataSetException {
        long time = System.currentTimeMillis();

        while (dataSetIterator.hasNext()) {
            ClassificationKey key = dataSetIterator.next();
            ClassificationKeyStub stub;
            try {
                stub = ClassificationKeyStub.fromClassificationKey(key, table);
            } catch (Exception e) {
                System.err.println("Warning: cannot compute key stub: " + e.getMessage());
                continue;
            }
            Long keyId = batchHolder.registerKey(key);
            keyIdToKeyStub.put(keyId, stub);
            dataSetSaver.registerKeyUnderKeyId(key, keyId);

            priorProbabilityEstimator.addMask(stub.getMask());

            if (keyId % 100000 == 100000 - 1) {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - time;
                System.out.println(String.format("Parsed %d (%d seconds, %d per second) %d MB memory usage", keyId + 1, elapsedTime / 1000, 100000 * 1000 / elapsedTime, Runtime.getRuntime().totalMemory() / 1000000));
                time = currentTime;
            }

        }
        dataSetIterator.close();

        time = System.currentTimeMillis();
        PriorProbability priorProbability = priorProbabilityEstimator.computePriorProbability();
        table.applyPriorProbability(priorProbability);
        System.out.println(String.format("Computed prior probability in %d seconds", (System.currentTimeMillis() - time) / 1000));

        time = System.currentTimeMillis();
        // TODO repeat for different prior probabilities
        List<Long> batchIds = batchHolder.getBatchIdsForKeyWithProperty();

        for (Long batchId : batchIds) {
            List<ClassificationKeyStub> stubs = batchHolder.getKeyIdsByBatchId(batchId).stream().map(keyId -> keyIdToKeyStub.get(keyId)).collect(Collectors.toList());
            ClassificationContainer container = classifyAsBatch(stubs);
            if (container == null) continue;
            statisticsAggregator.addStatistics(container);
            dataSetSaver.setBatchClassificationResult(batchId, container);
        }

        for (Long keyId : batchHolder.getKeyIdsWithoutProperty()) {
            ClassificationKeyStub stub = keyIdToKeyStub.get(keyId);
            ClassificationContainer container = classifyIndividually(stub);
            if (container == null) continue;
            statisticsAggregator.addStatistics(container);
            dataSetSaver.setBatchClassificationResult(batchHolder.getBatchIdForKeyId(keyId), container);
        }
        System.out.println(String.format("Classified all batches in %d seconds", (System.currentTimeMillis() - time) / 1000));

        time = System.currentTimeMillis();
        statisticsAggregator.saveStatistics();
        statisticsAggregator.savePriorProbabilitySummary(priorProbabilityEstimator);
        System.out.println(String.format("Saved statistics in %d seconds", (System.currentTimeMillis() - time) / 1000));

        time = System.currentTimeMillis();
        dataSetSaver.reconstructDataSet(batchHolder, keyIdToKeyStub);
        System.out.println(String.format("Saved annotated dataset in %d seconds", (System.currentTimeMillis() - time) / 1000));
    }

    private ClassificationContainer classifyAsBatch(List<ClassificationKeyStub> stubs) {
        ClassificationContainer container = null;
        for (ClassificationKeyStub stub : stubs) {
            ClassificationRow row = table.classifyIdentification(stub.getMask());
            if (row == null) {
                System.err.println("Warning: could not classify key with mask: " + stub.getMask());
                continue;
            }
            if (container == null) {
                container = new ClassificationContainer(stub.getDuplicityCount(), row);
            } else {
                container.add(stub.getDuplicityCount(), row);
            }
        }
        return container;
    }

    private ClassificationContainer classifyIndividually(ClassificationKeyStub stub) {
        ClassificationRow row = table.classifyIdentification(stub.getMask());
        if (row == null) {
            System.err.println("Warning: could not classify key with mask: " + stub.getMask());
            return null;
        }
        return new ClassificationContainer(stub.getDuplicityCount(), table.classifyIdentification(stub.getMask()));
    }
}
