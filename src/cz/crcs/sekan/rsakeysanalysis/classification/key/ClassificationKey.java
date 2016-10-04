package cz.crcs.sekan.rsakeysanalysis.classification.key;

import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;
import cz.crcs.sekan.rsakeysanalysis.common.exception.WrongKeyException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.math.BigInteger;
import java.util.*;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 02/10/2016
 */
public class ClassificationKey {
    /**
     * RSA key
     */
    private RSAKey rsaKey = new RSAKey();

    /**
     * Set of source's identifications used for creating batches during classification
     */
    private Set<String> source = null;

    /**
     * Is order of p and q of key known
     */
    private boolean ordered = false;

    /**
     * Number of duplicities of this key
     */
    private int count = 1;

    /**
     * Other information about key which will be not used during classification
     */
    private JSONObject info = null;

    /**
     * Factors of p-1, p+1, q-1, q+1
     */
    private List<ClassificationFactor> pmoFactors = null;
    private List<ClassificationFactor> ppoFactors = null;
    private List<ClassificationFactor> qmoFactors = null;
    private List<ClassificationFactor> qpoFactors = null;

    /**
     * Construct key from json object
     * @param json string contains json object of key
     * @throws ParseException Cannot parse json string
     * @throws WrongKeyException Key does not contain n or p and q
     */
    public ClassificationKey(String json) throws ParseException, WrongKeyException {
        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject)parser.parse(json);
        if (!object.containsKey("n") && (!object.containsKey("p") || !object.containsKey("q"))) {
            throw new WrongKeyException("Key does not contain n or p and q.");
        }
        if (object.containsKey("n")) rsaKey.setModulus(BigIntegerConversion.fromString((String)object.get("n")));
        if (object.containsKey("e")) rsaKey.setExponent(BigIntegerConversion.fromString((String)object.get("e")));
        if (object.containsKey("p")) rsaKey.setP(BigIntegerConversion.fromString((String)object.get("p")));
        if (object.containsKey("q")) rsaKey.setQ(BigIntegerConversion.fromString((String)object.get("q")));
        if (object.containsKey("ordered")) ordered = (Boolean)object.get("ordered");
        if (object.containsKey("count")) count = ((Number)object.get("count")).intValue();
        if (object.containsKey("source")) {
            source = new HashSet<>();
            JSONArray array = (JSONArray)object.get("source");
            for (Object sourcePart : array) {
                source.add((String)sourcePart);
            }
        }
        if (object.containsKey("info")) info = (JSONObject)object.get("info");
        if (object.containsKey("p-1 factors")) pmoFactors = parseArrayOfFactors((JSONArray)object.get("p-1 factors"));
        if (object.containsKey("p+1 factors")) ppoFactors = parseArrayOfFactors((JSONArray)object.get("p+1 factors"));
        if (object.containsKey("q-1 factors")) qmoFactors = parseArrayOfFactors((JSONArray)object.get("q-1 factors"));
        if (object.containsKey("q+1 factors")) qpoFactors = parseArrayOfFactors((JSONArray)object.get("q+1 factors"));
    }

    /**
     * For create a copy of key
     * @param otherKey key for create copy
     */
    private ClassificationKey(ClassificationKey otherKey) {
        this.rsaKey = otherKey.rsaKey;
        if (otherKey.source != null) {
            this.source = new HashSet<>();
            this.source.addAll(otherKey.source);
        }
        this.ordered = otherKey.ordered;
        this.count = otherKey.count;
        this.info = otherKey.info;
        if (otherKey.pmoFactors != null) {
            this.pmoFactors = new ArrayList<>();
            this.pmoFactors.addAll(otherKey.pmoFactors);
        }
        if (otherKey.ppoFactors != null) {
            this.ppoFactors = new ArrayList<>();
            this.ppoFactors.addAll(otherKey.ppoFactors);
        }
        if (otherKey.qmoFactors != null) {
            this.qmoFactors = new ArrayList<>();
            this.qmoFactors.addAll(otherKey.qmoFactors);
        }
        if (otherKey.qpoFactors != null) {
            this.qpoFactors = new ArrayList<>();
            this.qpoFactors.addAll(otherKey.qpoFactors);
        }
    }

    /**
     * Function for merge two keys if they have same rsa key
     * @param otherKey other key to merge with
     * @return new key with information from both of keys
     * @throws WrongKeyException Keys are not equals
     */
    public ClassificationKey mergeWith(ClassificationKey otherKey) throws WrongKeyException {
        if (!otherKey.getRsaKey().getModulus().equals(this.getRsaKey().getModulus())) {
            throw new WrongKeyException("Cannot merge two different keys.");
        }

        ClassificationKey key = new ClassificationKey(otherKey);
        key.setCount(key.getCount() + this.getCount());
        //If other key is ordered => save order to new key
        if (!key.ordered && this.ordered) {
            key.ordered = true;
            if (key.getRsaKey().getP().equals(this.getRsaKey().getQ()) &&
                    key.getRsaKey().getQ().equals(this.getRsaKey().getP())) {
                key.rsaKey = this.rsaKey;
                List<ClassificationFactor> tmp1, tmp2;
                tmp1 = key.pmoFactors;
                tmp2 = key.ppoFactors;
                key.pmoFactors = this.qmoFactors;
                key.ppoFactors = this.qpoFactors;
                key.qmoFactors = tmp1;
                key.qpoFactors = tmp2;
            }
        }

        if (key.pmoFactors == null && this.pmoFactors != null) {
            key.pmoFactors = new ArrayList<>();
            key.pmoFactors.addAll(this.pmoFactors);
        }
        if (key.ppoFactors == null && this.ppoFactors != null) {
            key.ppoFactors = new ArrayList<>();
            key.ppoFactors.addAll(this.ppoFactors);
        }
        if (key.qmoFactors == null && this.qmoFactors != null) {
            key.qmoFactors = new ArrayList<>();
            key.qmoFactors.addAll(this.qmoFactors);
        }
        if (key.qpoFactors == null && this.qpoFactors != null) {
            key.qpoFactors = new ArrayList<>();
            key.qpoFactors.addAll(this.qpoFactors);
        }

        if (key.getInfo() != null || this.getInfo() != null) {
            JSONObject infoA = (key.getInfo() != null ? key.getInfo() : this.getInfo());
            JSONObject infoB = (key.getInfo() != null ? this.getInfo() : key.getInfo());

            if (infoB != null) {
                for (Object obj : infoB.keySet()) {
                    if (!infoA.containsKey(obj)) {
                        infoA.put(obj, infoB.get(obj));
                    } else {
                        Object infoPartA = infoA.get(obj);
                        Object infoPartB = infoB.get(obj);
                        Set<Object> set = new HashSet<>();
                        if (infoPartA instanceof Collection) {
                            set.addAll((Collection<?>)infoPartA);
                        }
                        else {
                            set.add(infoPartA);
                        }
                        if (infoPartB instanceof Collection) {
                            set.addAll((Collection<?>)infoPartB);
                        }
                        else {
                            set.add(infoPartB);
                        }
                        JSONArray array = new JSONArray();
                        array.addAll(set);
                        infoA.remove(obj);
                        infoA.put(obj, array);
                    }
                }
            }
            key.setInfo(infoA);
        }
        if (key.getSource() != null || this.getSource() != null) {
            Set<String> sources = new HashSet<>();
            if (this.getSource() != null) sources.addAll(this.getSource());
            if (key.getSource() != null) sources.addAll(key.getSource());
            key.setSource(sources);
        }
        return key;
    }

    /**
     * Helper function for parse json array of classification factors
     * @param array json array of factors
     * @return list of parsed factors
     */
    protected List<ClassificationFactor> parseArrayOfFactors(JSONArray array) {
        List<ClassificationFactor> factors = new ArrayList<>();
        for (Object f : array) {
            JSONObject factor = (JSONObject)f;
            BigInteger tmpFactor = BigIntegerConversion.fromString((String)factor.get("factor"));
            long tmpPower = ((Number)factor.get("power")).longValue();
            factors.add(new ClassificationFactor(tmpFactor, tmpPower));
        }
        return factors;
    }

    /**
     * Helper function for build json object
     * @param factors list of factors
     * @return json array
     */
    protected JSONArray factorsToJSONArray(List<ClassificationFactor> factors) {
        JSONArray array = new JSONArray();
        for (ClassificationFactor factor : factors) {
            array.add(factor.toJSON());
        }
        return array;
    }

    /**
     * Convert to json object
     * @return json object
     */
    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        if (rsaKey.getModulus() != null) object.put("n", BigIntegerConversion.toString(rsaKey.getModulus()));
        if (rsaKey.getExponent() != null) object.put("e", BigIntegerConversion.toString(rsaKey.getExponent()));
        if (rsaKey.getP() != null) object.put("p", BigIntegerConversion.toString(rsaKey.getP()));
        if (rsaKey.getQ() != null) object.put("q", BigIntegerConversion.toString(rsaKey.getQ()));
        object.put("ordered", ordered);
        object.put("count", count);
        if (source != null) {
            JSONArray array = new JSONArray();
            array.addAll(source);
            object.put("source", array);
        }
        if (info != null) object.put("info", info);
        if (pmoFactors != null) object.put("p-1 factors", factorsToJSONArray(pmoFactors));
        if (ppoFactors != null) object.put("p+1 factors", factorsToJSONArray(ppoFactors));
        if (qmoFactors != null) object.put("q-1 factors", factorsToJSONArray(qmoFactors));
        if (qpoFactors != null) object.put("q+1 factors", factorsToJSONArray(qpoFactors));
        return object;
    }

    @Override
    public String toString() {
        return "ClassificationKey{" +
                "rsaKey=" + rsaKey +
                ", source=" + source +
                ", ordered=" + ordered +
                ", count=" + count +
                ", info=" + info +
                ", pmoFactors=" + pmoFactors +
                ", ppoFactors=" + ppoFactors +
                ", qmoFactors=" + qmoFactors +
                ", qpoFactors=" + qpoFactors +
                '}';
    }

    public RSAKey getRsaKey() {
        return rsaKey;
    }

    public Set<String> getSource() {
        return source;
    }

    public boolean isOrdered() {
        return ordered;
    }

    public int getCount() {
        return count;
    }

    public JSONObject getInfo() {
        return info;
    }

    public List<ClassificationFactor> getPmoFactors() {
        return pmoFactors;
    }

    public List<ClassificationFactor> getPpoFactors() {
        return ppoFactors;
    }

    public List<ClassificationFactor> getQmoFactors() {
        return qmoFactors;
    }

    public List<ClassificationFactor> getQpoFactors() {
        return qpoFactors;
    }

    public void setSource(Set<String> source) {
        this.source = source;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setInfo(JSONObject info) {
        this.info = info;
    }
}
