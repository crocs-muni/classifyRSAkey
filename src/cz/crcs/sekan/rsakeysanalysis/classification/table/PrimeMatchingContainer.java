package cz.crcs.sekan.rsakeysanalysis.classification.table;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;

import java.math.BigInteger;

/**
 * For batching keys by shared primes, we need to remember the primes and retain information from ClassificationContainer
 *
 * @author Matus Nemec
 * @version 19.10.2016
 */
public class PrimeMatchingContainer extends ClassificationContainer {

    private BigInteger p;
    private BigInteger q;

    public PrimeMatchingContainer(long numOfDuplicityKeys, ClassificationRow row, ClassificationKey key, BigInteger p, BigInteger q) {
        super(numOfDuplicityKeys, row, key);
        this.p = p;
        this.q = q;
    }

    public BigInteger getP() {
        return p;
    }

    public BigInteger getQ() {
        return q;
    }

    /**
     *
     * @param o
     * @return whether both the primes are equal and in the same order
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrimeMatchingContainer that = (PrimeMatchingContainer) o;

        return p != null ? p.equals(that.p) : that.p == null
                && (q != null ? q.equals(that.q) : that.q == null)
                && (getKeys() != null ? getKeys().equals(that.getKeys()) : that.getKeys() == null);
    }

    @Override
    public int hashCode() {
        int result = p != null ? p.hashCode() : 0;
        result = 31 * result + (q != null ? q.hashCode() : 0);
        return result;
    }
}
