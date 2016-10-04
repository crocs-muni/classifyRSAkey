package cz.crcs.sekan.rsakeysanalysis.classification.table.transformation;

import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongOptionsFormatException;
import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 27/09/2016
 */
public class ExpectedRemainderTransformation extends Transformation {
    /**
     * Set of dividers on part of key
     */
    private Set<BigInteger> dividers = new HashSet<>();

    /**
     * Number which we want to get for transform part of key to bit 1, otherwise transform par of key to bit 0.
     */
    private BigInteger expectedRemainder = null;

    public ExpectedRemainderTransformation(RSAKey.PART from, JSONObject options) throws WrongOptionsFormatException {
        super(from, options);
        if (!options.containsKey("dividers") ||
            !(options.get("dividers") instanceof JSONArray) ||
            !options.containsKey("expectedRemainder")) {
            throw new WrongOptionsFormatException("Transformation ExpectedRemainder has not completely or correct options.");
        }

        JSONArray ds = (JSONArray) options.get("dividers");
        for (Object divider : ds) {
            BigInteger d = BigInteger.valueOf(((Number)divider).longValue());
            dividers.add(d);
        }
        expectedRemainder = BigInteger.valueOf(((Number)getRequiredOption("expectedRemainder")).longValue());
    }

    @Override
    public String transform(RSAKey key) {
        for (BigInteger divider : dividers) {
            if (key.getPart(from).mod(divider).compareTo(expectedRemainder) == 0) {
                return "1";
            }
        }
        return "0";
    }
}
