package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author xnemec1
 * @version 11/28/16.
 */
public class CsvDataSetFormatter implements DataSetFormatter {

    public CsvDataSetFormatter() {
        throw new NotImplementedException();  // TODO implement
    }

    @Override
    public String classifiedKeyToLine(ClassificationKey key, ClassificationContainer container) {
        return null;
    }

    @Override
    public String originalKeyToLine(ClassificationKey key) {
        return null;
    }
}
