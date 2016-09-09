package cz.crcs.sekan.rsakeysanalysis.classification.table.transformation;

import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongOptionsFormatException;
import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;
import org.json.simple.JSONObject;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 18.04.2016
 */
public class MostSignificantBitsTransformation extends Transformation {
    /**
     * Number of bits to extract from part of rsa key
     */
    private int bits;

    /**
     * Number of bits to skip from part of rsa key
     */
    private int skip = 1;

    public MostSignificantBitsTransformation(RSAKey.PART from, JSONObject options) throws WrongOptionsFormatException {
        super(from, options);
        if (options.containsKey("skip")) {
            skip = ((Number)options.get("skip")).intValue();
        }
        bits = ((Number)getRequiredOption("bits")).intValue();
    }

    @Override
    public String transform(RSAKey key) {
        return key.getPart(from).toString(2).substring(skip, bits + skip);
    }
}
