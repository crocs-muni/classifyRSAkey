package cz.crcs.sekan.rsakeysanalysis.common;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;

import java.math.BigInteger;
import java.util.Set;
import java.util.TreeSet;

/**
 * Iterates through file obtaining moduli, only outputs each modulus once
 *
 * @author xnemec1
 * @version 3/20/17.
 */
public class UniqueModulusIterator extends ModulusIterator {

    private Set<BigInteger> seenModuli;
    private BigInteger nextModulus;

    public UniqueModulusIterator(FileIterator fileIterator) {
        super(fileIterator);
        this.seenModuli = new TreeSet<>();
        this.nextModulus = null;
    }

    @Override
    public boolean hasNext() {
        if (nextModulus != null) return true;
        if (!super.hasNext()) return false;
        while (nextModulus == null && super.hasNext()) {
            BigInteger nextNonUniqueModulus = super.next();
            if (!seenModuli.add(ClassificationKey.shortenModulus(nextNonUniqueModulus))) continue;
            nextModulus = nextNonUniqueModulus;
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
