package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.BatchHolder;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKeyStub;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;

import java.util.Map;

/**
 * @author xnemec1
 * @version 11/24/16.
 */
public interface DataSetSaver {
    public void registerKeyUnderKeyId(ClassificationKey key, Long keyId);
    public void setBatchClassificationResult(Long batchId, ClassificationContainer classificationContainer);
    public void reconstructDataSet(BatchHolder batchHolder, Map<Long, ClassificationKeyStub> keyIdToKeyStub);
}
