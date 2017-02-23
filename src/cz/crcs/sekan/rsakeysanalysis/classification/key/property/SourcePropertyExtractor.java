package cz.crcs.sekan.rsakeysanalysis.classification.key.property;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author xnemec1
 * @version 11/22/16.
 */
public class SourcePropertyExtractor implements PropertyExtractor<Set<String>> {
    @Override
    public List<Set<String>> extractProperty(ClassificationKey key) {
        List<Set<String>> sourceSetList = new ArrayList<>(1);
        if (key.getSource() != null && !key.getSource().isEmpty()) sourceSetList.add(key.getSource());
        return sourceSetList;
    }
}
