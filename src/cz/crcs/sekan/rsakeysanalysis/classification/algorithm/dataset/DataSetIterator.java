package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;

import java.util.Iterator;

/**
 * @author xnemec1
 * @version 11/23/16.
 */
public interface DataSetIterator extends Iterator<ClassificationKey> {
    public void close();

    public String getDataSetName();
}
