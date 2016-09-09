package cz.crcs.sekan.rsakeysanalysis.classification.tests;

import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 12.06.2016
 */
public class ModulusFactors {
    private Set<BigInteger> factors = new HashSet<>();

    public ModulusFactors(String file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("0x")) line = line.substring(2);
                if (line.length() == 0) continue;
                factors.add(RSAKey.stringToBigInteger(line));
            }
        }
        catch (IOException ex) {
            throw new IllegalArgumentException("ModulusFactors cannot be construct. Some IOException occurs.", ex);
        }
    }

    /**
     * Get factor of modulus
     * @param key RSA key
     * @return null|factor
     */
    public BigInteger getFactor(RSAKey key) {
        for (BigInteger factor : factors) {
            if (key.getModulus().mod(factor).compareTo(BigInteger.ZERO) == 0) {
                return factor;
            }
        }
        return null;
    }
}
