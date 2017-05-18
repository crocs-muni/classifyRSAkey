package cz.crcs.sekan.rsakeysanalysis.classification.key.property;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A property extractor where we do not care about batching but still need to hold information about keys
 *
 * @author xnemec1
 * @version 5/5/17.
 */
public class ModulusHashPropertyExtractor implements PropertyExtractor<BigInteger> {
    @Override
    public List<BigInteger> extractProperty(ClassificationKey key) {
        return Collections.singletonList(key.getShortenedModulus());
    }
}
