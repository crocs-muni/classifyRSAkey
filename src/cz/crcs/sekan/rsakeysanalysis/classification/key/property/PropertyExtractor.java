package cz.crcs.sekan.rsakeysanalysis.classification.key.property;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;

import java.util.List;

/**
 * @author xnemec1
 * @version 11/22/16.
 */
public interface PropertyExtractor<Property> {
    /**
     * Extracts properties for batching together ClassificationKey objects.
     * @param key
     * @return List of properties or EMPTY LIST if the key has no properties.
     */
    public List<Property> extractProperty(ClassificationKey key);
}
