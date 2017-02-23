package cz.crcs.sekan.rsakeysanalysis.classification.key.property;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xnemec1
 * @version 11/22/16.
 */
public class PrimePropertyExtractor implements PropertyExtractor<BigInteger> {
    @Override
    public List<BigInteger> extractProperty(ClassificationKey key) {
        List<BigInteger> primes = new ArrayList<>(2);
        if (key.getRsaKey().getP() != null) primes.add(key.getRsaKey().getP());
        if (key.getRsaKey().getQ() != null) primes.add(key.getRsaKey().getQ());
        return primes;
    }
}
