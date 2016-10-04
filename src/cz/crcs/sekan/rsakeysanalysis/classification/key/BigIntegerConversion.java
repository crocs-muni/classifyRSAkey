package cz.crcs.sekan.rsakeysanalysis.classification.key;

import java.math.BigInteger;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 01/10/2016
 */
public class BigIntegerConversion {
    public static String toString(BigInteger number) {
        return "0x" + number.toString(16);
    }

    public static BigInteger fromString(String number) {
        if (number.startsWith("0x")) {
            return new BigInteger(number.substring(2), 16);
        }
        else if (number.startsWith("0b")) {
            return new BigInteger(number.substring(2), 2);
        }
        else {
            return new BigInteger(number, 10);
        }
    }
}
