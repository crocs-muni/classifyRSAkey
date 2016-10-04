package cz.crcs.sekan.rsakeysanalysis.classification.key;

import org.json.simple.JSONObject;

import java.math.BigInteger;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 02/10/2016
 */
public class ClassificationFactor {
    private BigInteger factor;

    private long power;

    public ClassificationFactor(BigInteger factor, long power) {
        this.factor = factor;
        this.power = power;
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("factor", BigIntegerConversion.toString(factor));
        object.put("power", power);
        return object;
    }

    public BigInteger getFactor() {
        return factor;
    }

    public long getPower() {
        return power;
    }
}
