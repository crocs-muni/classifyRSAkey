package cz.crcs.sekan.rsakeysanalysis.classification.algorithm;

import com.sun.xml.internal.bind.v2.runtime.IllegalAnnotationException;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.*;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset.*;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics.BatchesStatisticsAggregator;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics.StatisticsAggregator;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKeyStub;
import cz.crcs.sekan.rsakeysanalysis.classification.key.property.ModulusHashPropertyExtractor;
import cz.crcs.sekan.rsakeysanalysis.classification.key.property.PrimePropertyExtractor;
import cz.crcs.sekan.rsakeysanalysis.classification.key.property.PropertyExtractor;
import cz.crcs.sekan.rsakeysanalysis.classification.key.property.SourcePropertyExtractor;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
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

    private boolean makeOutputs;

    private boolean onlyPriorEstimation;


    private Map<Long, ClassificationKeyStub> keyIdToKeyStub;

    private BatchHolder<BatchProperty> batchHolder;

    private Classification() {
        makeOutputs = false;
    }

    public enum BatchType {
        SOURCE("source"),
        PRIMES("primes"),
        MODULUS_HASH("modulus_hash"),
        NONE("none");

        private final String name;

        BatchType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum PriorType {
        ESTIMATE("estimate"),
        UNIFORM("uniform"),
        TABLE("table");

        private final String name;

        PriorType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum ExportType {
        NONE("none"),
        JSON("json"),
        CSV("csv");

        private final String name;

        ExportType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum MemoryType {
        NONE("none"),
        DISK("disk"),
        MEMORY("memory");

        private final String name;

        MemoryType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
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

        public Builder<BatchProperty> makeOutputs() {
            classification.makeOutputs = true;
            return this;
        }

        public Builder<BatchProperty> onlyPriorEstimation() {
            classification.onlyPriorEstimation = true;
            return this;
        }

        public Classification<BatchProperty> build() {
            classification.keyIdToKeyStub = new TreeMap<>();
            if (classification.propertyExtractor != null) {
                classification.batchHolder = new BatchHolder<>(classification.propertyExtractor);
            } else {
                classification.batchHolder = null;
            }
            return classification;
        }
    }

    public static class BuildHelper {

        public static Builder prepareBuilder(ClassificationConfiguration config, String datasetFilePath) throws IOException, DataSetException {
            Classification.Builder builder;

            switch (config.batchType) {
                case SOURCE:
                    builder = new Classification.Builder<Set<String>>();
                    builder.setPropertyExtractor(new SourcePropertyExtractor());
                    break;
                case PRIMES:
                    builder = new Classification.Builder<BigInteger>();
                    builder.setPropertyExtractor(new PrimePropertyExtractor());
                    break;
                case MODULUS_HASH:
                    builder = new Classification.Builder<BigInteger>();
                    builder.setPropertyExtractor(new ModulusHashPropertyExtractor());
                    break;
                case NONE:
                    builder = new Classification.Builder<Object>();
                    builder.setPropertyExtractor(null);
                    break;
                default:
                    throw new NotImplementedException();
            }

            PriorProbabilityEstimator estimator;

            switch (config.priorType) {
                case ESTIMATE:
                    estimator = new NonNegativeLeastSquaresFitPriorProbabilityEstimator(config.classificationTable);
                    break;
                case UNIFORM:
                    estimator = new UniformPriorProbabilityEstimator(config.classificationTable);
                    break;
                case TABLE:
                    estimator = new UserDefinedPriorProbabilityEstimator(config.classificationTable);
                    break;
                default:
                    throw new NotImplementedException();
            }

            DataSetFormatter formatter = null;
            DataSetSaver dataSetSaver;

            switch (config.exportType) {
                case CSV:
                    formatter = new CsvDataSetFormatter();
                    break;
                case JSON:
                    formatter = new JsonDataSetFormatter();
                    break;
                case NONE:
                    formatter = null;
                    break;
                default:
                    throw new NotImplementedException();
            }

            DataSetIterator iterator = null;
            if (datasetFilePath != null) iterator = new FileDataSetIterator(datasetFilePath);
            builder.setDataSetIterator(iterator);

            if (formatter == null) {
                dataSetSaver = new NoActionDataSetSaver();
            } else {
                ExtendedWriter datasetWriter = new ExtendedWriter(new File(config.outputFolderPath,
                        "dataset_" + getDataSetName(iterator)));
                switch (config.memoryType) {
                    case DISK:
                        dataSetSaver = new FromFileDataSetSaver(new FileDataSetIterator(datasetFilePath), formatter, datasetWriter);
                        break;
                    case MEMORY:
                        dataSetSaver = new InMemoryDataSetSaver(formatter, datasetWriter);
                        break;
                    case NONE:
                        dataSetSaver = new NoActionDataSetSaver();
                        break;
                    default:
                        throw new NotImplementedException();
                }
            }

            builder.setDataSetSaver(dataSetSaver);
            builder.setPriorProbabilityEstimator(estimator);

            File outputDirectory = iterator == null ? new File(config.outputFolderPath)
                    : new File(config.outputFolderPath, iterator.getDataSetName());
            if (outputDirectory.exists()) {
                if (!outputDirectory.isDirectory())
                    throw new IllegalArgumentException("Output path already exists and is not a directory "
                            + outputDirectory.toString());
            } else {
                if (!outputDirectory.mkdir())
                    throw new IllegalArgumentException("Could not create directory " + outputDirectory.toString());
            }

            builder.setStatisticsAggregator(new BatchesStatisticsAggregator(new ArrayList<>(
                    config.classificationTable.getGroupsNames()), outputDirectory.getPath()));
            builder.setTable(config.classificationTable);
            if (config.makeOutputs) builder.makeOutputs();
            if (config.onlyPriorProbability) builder.onlyPriorEstimation();

            return builder;
        }
    }

    private static String getDataSetName(DataSetIterator iterator) {
        return iterator == null ? "dataset.json" : iterator.getDataSetName();
    }

    public String getDataSetName() {
        return getDataSetName(dataSetIterator);
    }

    public PriorProbabilityEstimator classify() throws DataSetException {
        long time = System.currentTimeMillis();

        Long keyId = 0L;

        while (dataSetIterator.hasNext()) {
            ClassificationKey key = dataSetIterator.next();
            ClassificationKeyStub stub;
            try {
                stub = ClassificationKeyStub.fromClassificationKey(key, table);
            } catch (Exception e) {
                System.err.println("Warning: cannot compute key stub: " + e.getMessage());
                continue;
            }
            if (!onlyPriorEstimation && batchHolder != null) {
                keyId = batchHolder.registerKey(key);
                keyIdToKeyStub.put(keyId, stub);
                dataSetSaver.registerKeyUnderKeyId(key, keyId);
            } else {
                keyId++;
            }

            priorProbabilityEstimator.addMask(stub.getMask());

            if (makeOutputs && keyId % 100000 == 100000 - 1) {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - time;
                System.out.println(String.format("Parsed %d (%d seconds, %d per second) %d MB memory usage", keyId + 1, elapsedTime / 1000, 100000 * 1000 / elapsedTime, Runtime.getRuntime().totalMemory() / 1000000));
                time = currentTime;
            }

        }
        dataSetIterator.close();

        time = System.currentTimeMillis();
        PriorProbability priorProbability = priorProbabilityEstimator.computePriorProbability();

        statisticsAggregator.savePriorProbabilitySummary(priorProbabilityEstimator);
        if (onlyPriorEstimation || batchHolder == null) {
            return priorProbabilityEstimator;
        }

        table.applyPriorProbability(priorProbability);
        if (makeOutputs)
            System.out.println(String.format("Computed prior probability in %d seconds", (System.currentTimeMillis() - time) / 1000));

        time = System.currentTimeMillis();
        // TODO repeat for different prior probabilities
        List<Long> batchIds = batchHolder.getBatchIdsForKeyWithProperty();

        for (Long batchId : batchIds) {
            List<ClassificationKeyStub> stubs = batchHolder.getKeyIdsByBatchId(batchId).stream().map(
                    keyInBatchId -> keyIdToKeyStub.get(keyInBatchId)).collect(Collectors.toList());
            ClassificationContainer container = classifyAsBatch(stubs);
            if (container == null) continue;
            statisticsAggregator.addStatistics(container, stubs.toArray(new ClassificationKeyStub[stubs.size()]));
            dataSetSaver.setBatchClassificationResult(batchId, container);
        }

        for (Long keyIdNoProperty : batchHolder.getKeyIdsWithoutProperty()) {
            ClassificationKeyStub stub = keyIdToKeyStub.get(keyIdNoProperty);
            ClassificationContainer container = classifyIndividually(stub);
            if (container == null) continue;
            statisticsAggregator.addStatistics(container, stub);
            dataSetSaver.setBatchClassificationResult(batchHolder.getBatchIdForKeyId(keyIdNoProperty), container);
        }
        if (makeOutputs)
            System.out.println(String.format("Classified all batches in %d seconds", (System.currentTimeMillis() - time) / 1000));

        time = System.currentTimeMillis();
        statisticsAggregator.saveStatistics();

        if (makeOutputs)
            System.out.println(String.format("Saved statistics in %d seconds", (System.currentTimeMillis() - time) / 1000));

        time = System.currentTimeMillis();
        dataSetSaver.reconstructDataSet(batchHolder, keyIdToKeyStub);
        if (makeOutputs)
            System.out.println(String.format("Saved annotated dataset in %d seconds", (System.currentTimeMillis() - time) / 1000));
        return priorProbabilityEstimator;
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
