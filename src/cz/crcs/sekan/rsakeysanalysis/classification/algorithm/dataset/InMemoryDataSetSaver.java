package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.BatchHolder;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKeyStub;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author xnemec1
 * @version 11/24/16.
 */
public class InMemoryDataSetSaver implements DataSetSaver {

    private Map<Long, ClassificationKey> keyIdToClassificationKey;
    private Map<Long, ClassificationContainer> batchIdToClassificationContainer;
    private DataSetFormatter dataSetFormatter;
    private ExtendedWriter resultWriter;

    public InMemoryDataSetSaver(DataSetFormatter dataSetFormatter, ExtendedWriter resultWriter) {
        this.dataSetFormatter = dataSetFormatter;
        this.resultWriter = resultWriter;
        keyIdToClassificationKey = new TreeMap<>();
        batchIdToClassificationContainer = new TreeMap<>();
    }

    @Override
    public void registerKeyUnderKeyId(ClassificationKey key, Long keyId) {
        keyIdToClassificationKey.put(keyId, key);
    }

    @Override
    public void setBatchClassificationResult(Long batchId, ClassificationContainer classificationContainer) {
        batchIdToClassificationContainer.put(batchId, classificationContainer);
    }

    @Override
    public void reconstructDataSet(BatchHolder batchHolder, Map<Long, ClassificationKeyStub> keyIdToKeyStub) throws IOException {
        for (Long keyId : keyIdToClassificationKey.keySet()) {
            ClassificationKey key = keyIdToClassificationKey.get(keyId);
            ClassificationContainer container = batchIdToClassificationContainer.get(batchHolder.getBatchIdForKeyId(keyId));
            ClassificationKeyStub stub = keyIdToKeyStub.get(keyId);
            if (stub != null) key.setIdentification(stub.getMask());
            try {
                resultWriter.writeln(dataSetFormatter.classifiedKeyToLine(key, container));
            } catch (IOException e) {
                System.err.println("Could not save result of classification: " + e.getMessage());
            }
        }
        resultWriter.flush();
    }
}
