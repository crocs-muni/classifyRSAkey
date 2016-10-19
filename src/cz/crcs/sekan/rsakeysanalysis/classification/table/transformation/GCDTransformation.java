package cz.crcs.sekan.rsakeysanalysis.classification.table.transformation;

import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongOptionsFormatException;
import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;
import org.json.simple.JSONObject;

import java.math.BigInteger;

/**
 * @author Matus Nemec
 * @version 19.10.2016
 */
public class GCDTransformation extends Transformation {

    private BigInteger expectedCommonDivisor;
    private BigInteger dividend;

    public GCDTransformation(RSAKey.PART from, JSONObject options) throws WrongOptionsFormatException {
        super(from, options);
        expectedCommonDivisor = BigInteger.valueOf(((Number) getRequiredOption("expectedCommonDivisor")).intValue());
        dividend = new BigInteger((String) getRequiredOption("dividend"));
    }

    @Override
    public String transform(RSAKey key) {
        return key.getPart(from).subtract(BigInteger.ONE).gcd(dividend).equals(expectedCommonDivisor) ? "0": "1";
    }

}
