package cz.crcs.sekan.rsakeysanalysis.common;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;

import java.io.File;
import java.math.BigInteger;
import java.util.Iterator;

/**
 * Iterate through file obtaining moduli
 *
 * @author xnemec1
 * @version 3/20/17.
 */
public class ModulusIterator implements Iterator<BigInteger> {

    private FileIterator fileIterator;
    private BigInteger nextModulus;

    public ModulusIterator(FileIterator fileIterator) {
        this.fileIterator = fileIterator;
        nextModulus = null;
    }

    @Override
    public boolean hasNext() {
        if (nextModulus != null) return true;
        if (!fileIterator.hasNext()) return false;
        while (nextModulus == null && fileIterator.hasNext()) {
            nextModulus = ClassificationKey.getModulusFromJSON(fileIterator.next());
        }
        return nextModulus != null;
    }

    @Override
    public BigInteger next() {
        if (hasNext()) {
            BigInteger returnModulus = nextModulus;
            nextModulus = null;
            return returnModulus;
        }
        return null;
    }
}
