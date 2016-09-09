package cz.crcs.sekan.rsakeysanalysis.classification.table.transformation;

import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongOptionsFormatException;
import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;
import org.json.simple.JSONObject;

import java.math.BigInteger;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 18.04.2016
 */
public class RemainderFromDivisionTransformation extends Transformation {
    /**
     * Divider for modulo
     */
    private int divisor;

    public RemainderFromDivisionTransformation(RSAKey.PART from, JSONObject options) throws WrongOptionsFormatException {
        super(from, options);
        divisor = ((Number)getRequiredOption("divisor")).intValue();
    }

    @Override
    public String transform(RSAKey key) {
        return key.getPart(from).mod(BigInteger.valueOf(divisor)).toString(10);
    }
}
