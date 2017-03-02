package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;

/**
 * @author xnemec1
 * @version 11/28/16.
 */
public interface DataSetFormatter {
    public String classifiedKeyToLine(ClassificationKey key, ClassificationContainer container);
    public String originalKeyToLine(ClassificationKey key);
}
