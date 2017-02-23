package cz.crcs.sekan.rsakeysanalysis.classification.algorithm;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.key.property.PropertyExtractor;
import cz.crcs.sekan.rsakeysanalysis.common.BidirectionalMap;


import java.util.*;

/**
 * @author xnemec1
 * @version 11/22/16.
 */
public class BatchHolder<Property> {

    private Long nextBatchId;
    private Long nextKeyId;

    private BidirectionalMap<Long, Property> batchIdToProperties;
    private BidirectionalMap<Long, Long> batchIdToKeyIds;
    private PropertyExtractor<Property> propertyExtractor;

    private List<Long> keyIdsWithoutProperty;

    public BatchHolder(PropertyExtractor<Property> propertyExtractor) {
        batchIdToProperties = new BidirectionalMap<>();
        batchIdToKeyIds = new BidirectionalMap<>();
        nextBatchId = 0L;
        nextKeyId = 0L;
        this.propertyExtractor = propertyExtractor;
        keyIdsWithoutProperty = new LinkedList<>();
    }

    /**
     * @param key
     * @return the unique ID under which the key will be registered
     */
    public Long registerKey(ClassificationKey key) {
        return registerKeyIdWithProperties(nextKeyId++, propertyExtractor.extractProperty(key));
    }

    private Long registerKeyIdWithProperties(Long keyId, List<Property> properties) {
        if (properties == null) properties = new ArrayList<Property>();

        if (properties.isEmpty()) {
            keyIdsWithoutProperty.add(keyId);
            batchIdToKeyIds.placeValuesUnderSameKey(Collections.singletonList(keyId), nextBatchId);
        } else {
            List<Long> joinedGroups = batchIdToProperties.placeValuesUnderSameKey(properties, nextBatchId);
            batchIdToKeyIds.placeValuesUnderSameKey(Collections.singletonList(keyId), nextBatchId);
            if (joinedGroups.size() != 1 || !nextBatchId.equals(joinedGroups.get(0))) {
                joinedGroups.add(nextBatchId);
                batchIdToKeyIds.joinBatchesUnderNewKey(joinedGroups, nextBatchId);
            }
        }

        nextBatchId++;
        return keyId;
    }

    public List<Long> getBatchIdsForKeyWithProperty() {
        return batchIdToKeyIds.getKeys();
    }

    public List<Long> getKeyIdsByBatchId(Long batchId) {
        return batchIdToKeyIds.getValuesByKey(batchId);
    }

    public List<Long> getKeyIdsWithoutProperty() {
        return keyIdsWithoutProperty;
    }

    public Long getBatchIdForKeyId(Long keyId) {
        return batchIdToKeyIds.getKeyByValue(keyId);
    }
}
