package cz.crcs.sekan.rsakeysanalysis.classification.key;

import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;
import cz.crcs.sekan.rsakeysanalysis.common.exception.WrongKeyException;
import cz.crcs.sekan.rsakeysanalysis.template.Template;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private String identification;

    public String getIdentification() {
        return identification;
    }

    public void setIdentification(String identification) {
        this.identification = identification;
    }

    /**
     * Factors of p-1, p+1, q-1, q+1
     */
    private List<ClassificationFactor> pmoFactors = null;
    private List<ClassificationFactor> ppoFactors = null;
    private List<ClassificationFactor> qmoFactors = null;
    private List<ClassificationFactor> qpoFactors = null;

    /**
     * Used for classification success test -- real source which generated this key.
     */
    private String realSource;

    public String getRealSource() {
        return realSource;
    }

    private ClassificationKey() {
    }

    public ClassificationKey(RSAKey rsaKey, Set<String> source, int count, String identification, String realSource) {
        this.rsaKey = rsaKey;
        this.source = source;
        this.count = count;
        this.identification = identification;
        this.realSource = realSource;
    }

    /**
     * Construct key from json object
     * @param json string contains json object of key
     * @throws ParseException Cannot parse json string
     * @throws WrongKeyException Key does not contain n or p and q
     */
    public static ClassificationKey fromJson(String json) throws ParseException, WrongKeyException {
        if (isNewJsonFormat(json)) {
            return fromNewJsonFormat(json);
        } else {
            return fromOldJsonFormat(json);
        }
    }

    private static boolean isNewJsonFormat(String json) throws ParseException {
        return !json.contains("\"modulus\"");
    }

    public static BigInteger shortenModulus(BigInteger modulus) {
        if (modulus == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte hash[] = digest.digest(modulus.toByteArray());
            return new BigInteger(hash);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 not available");
            throw new RuntimeException(e);
        }
    }

    private static final Pattern patternModulus = Pattern.compile("\"(n|modulus)\" *: *\"(0x|0X)?([a-zA-Z0-9]+)\"");

    public static BigInteger getModulusFromJSON(String json) {
        if (json == null) return null;
        Matcher m = patternModulus.matcher(json);
        if (m.find()) {
            String matched = m.group(0);
            String[] split = matched.split("\"");
            if (split.length >= 4) {
                matched = split[3];
                if (matched.startsWith("0x") || matched.startsWith("0X")) matched = matched.substring(2);
            } else {
                System.err.println("No modulus present in json");
                return null;
            }
            return new BigInteger(matched, 16);
        }
        return null;
    }

    public static BigInteger getShortenedModulusFromJSON(String json) {
        return shortenModulus(getModulusFromJSON(json));
    }

    public BigInteger getShortenedModulus() {
        if (rsaKey == null || rsaKey.getModulus() == null) {
            return null;
        }
        return shortenModulus(rsaKey.getModulus());
    }

    public static ClassificationKey fromNewJsonFormat(String json) throws ParseException, WrongKeyException {
        ClassificationKey key = new ClassificationKey();

        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject)parser.parse(json);
        if (!isNewJsonFormat(json)) {
            throw new WrongKeyException("Key does not contain n or p and q.");
        }
        if (object.containsKey("n")) key.rsaKey.setModulus(BigIntegerConversion.fromString((String)object.get("n")));
        if (object.containsKey("e")) key.rsaKey.setExponent(BigIntegerConversion.fromString((String)object.get("e")));
        if (object.containsKey("p")) key.rsaKey.setP(BigIntegerConversion.fromString((String)object.get("p")));
        if (object.containsKey("q")) key.rsaKey.setQ(BigIntegerConversion.fromString((String)object.get("q")));
        if (object.containsKey("ordered")) key.ordered = (Boolean)object.get("ordered");
        if (object.containsKey("count")) key.count = ((Number)object.get("count")).intValue();
        if (object.containsKey("source")) {
            key.source = new CopyOnWriteArraySet<>();
            JSONArray array = (JSONArray)object.get("source");
            for (Object sourcePart : array) {
                key.source.add((String)sourcePart);
            }
        }
        if (object.containsKey("info")) key.info = (JSONObject)object.get("info");
        if (object.containsKey("p-1 factors")) key.pmoFactors = parseArrayOfFactors((JSONArray)object.get("p-1 factors"));
        if (object.containsKey("p+1 factors")) key.ppoFactors = parseArrayOfFactors((JSONArray)object.get("p+1 factors"));
        if (object.containsKey("q-1 factors")) key.qmoFactors = parseArrayOfFactors((JSONArray)object.get("q-1 factors"));
        if (object.containsKey("q+1 factors")) key.qpoFactors = parseArrayOfFactors((JSONArray)object.get("q+1 factors"));

        return key;
    }

    public static ClassificationKey fromOldJsonFormat(String json) throws ParseException, WrongKeyException {
        ClassificationKey key = new ClassificationKey();

        JSONParser parser = new JSONParser();
        String subjectId = "common_name";
        //Check if certificate has a valid json format (rsa_public_key, subject and validity property is needed)
        JSONObject obj = (JSONObject) parser.parse(json);

        //Read all needed information about certificate
        //Property count is not necessary, represent number of duplicities in source key set
        Number countNumber = (Number) obj.getOrDefault("count", 1);
        int count = countNumber.intValue();

        //Property validity has to have property start with date
        //If date has W3C date and time format or similar, only date is extracted
        JSONObject validity = (JSONObject) obj.getOrDefault("validity", null);
        if (validity == null) throw new WrongKeyException("Key does not contain validity");
        String validityStart = (String) validity.get("start");
        String validityStartByDay = validityStart;
        if (validityStart.contains("T")) {
            validityStartByDay = validityStart.split("T")[0];
        }

        //Property rsa_public_key has to have properties modulus and exponent
        JSONObject rsaPublicKey = (JSONObject) obj.getOrDefault("rsa_public_key", null);
        if (rsaPublicKey == null) throw new WrongKeyException("Key does not contain rsa_public_key");
        String modulus = (String) rsaPublicKey.getOrDefault("modulus", null);
        Object exponentObject = rsaPublicKey.getOrDefault("exponent", null);
        BigInteger exponent;
        try {
            exponent = BigInteger.valueOf(((Number) exponentObject).longValue());
        } catch (ClassCastException ex) {
            exponent = new BigInteger((String) exponentObject, 16);
        }

        //Property subject has to have property common_name
        JSONObject subject = (JSONObject) obj.getOrDefault("subject", null);
        if (subject == null || !subject.containsKey(subjectId)) throw new WrongKeyException("Key does not contain subject.");
        String subjectIdValue = subject.get(subjectId).toString();

        key.source = new CopyOnWriteArraySet<>();
        key.source.add(subjectIdValue);
        key.source.add(validityStartByDay);

        key.count = count;

        //Create public key object
        key.rsaKey = new RSAKey(modulus, exponent);

        key.info = obj;

        return key;
    }

    public String toStringByTemplate(Template template) {
        template.resetVariables();
        if (getRsaKey().getP() != null) template.setVariable("p", getRsaKey().getP().toString(16));
        if (getRsaKey().getQ() != null) template.setVariable("q", getRsaKey().getQ().toString(16));
        if (getRsaKey().getModulus() != null) template.setVariable("n", getRsaKey().getModulus().toString(16));
        template.setVariable("ordered", Boolean.valueOf(isOrdered()).toString());
        template.setVariable("occurrence", Long.valueOf(getCount()).toString());
        if (getSource() != null) template.setVariable("source", getSource().toString());
        if (getInfo() != null) template.setVariable("info", getInfo().toString());
        return template.generateString();
    }

    /**
     * For create a copy of key
     * @param otherKey key for create copy
     */
    private ClassificationKey(ClassificationKey otherKey) {
        this.rsaKey = otherKey.rsaKey;
        if (otherKey.source != null) {
            this.source = new CopyOnWriteArraySet<>();
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

    public enum KeyMergeStrategy {
        COLLAPSE_INFO,
        APPEND_INFO,
        IGNORE_INFO
    }

    /**
     * Merge otherKey into this key
     * @param otherKey
     * @param keyMergeStrategy COLLAPSE - put info into a set of unique elements - slow; APPEND - create an array of infos; IGNORE - do not process info
     * @throws WrongKeyException
     */
    public void mergeNoCopy(ClassificationKey otherKey, KeyMergeStrategy keyMergeStrategy) throws WrongKeyException {
        if (!otherKey.getRsaKey().getModulus().equals(this.getRsaKey().getModulus())) {
            throw new WrongKeyException("Cannot merge two different keys.");
        }
        this.setCount(otherKey.getCount() + this.getCount());
        //If other key is ordered => save order to new key
        if (otherKey.ordered && !this.ordered) {
            this.ordered = true;

            if (otherKey.getRsaKey().getP().equals(this.getRsaKey().getQ()) &&
                    otherKey.getRsaKey().getQ().equals(this.getRsaKey().getP())) {
                BigInteger tempPrime;
                tempPrime = this.getRsaKey().getP();
                this.getRsaKey().setP(this.getRsaKey().getQ());
                this.getRsaKey().setQ(tempPrime);
                List<ClassificationFactor> tmp = this.pmoFactors;
                this.pmoFactors = this.qmoFactors;
                this.qmoFactors = tmp;
                tmp = this.ppoFactors;
                this.ppoFactors = this.qpoFactors;
                this.qpoFactors = tmp;
            }
        }

        if (this.pmoFactors == null && otherKey.pmoFactors != null) {
            this.pmoFactors = otherKey.pmoFactors;
        }
        if (this.ppoFactors == null && otherKey.ppoFactors != null) {
            this.ppoFactors = otherKey.ppoFactors;
        }
        if (this.qmoFactors == null && otherKey.qmoFactors != null) {
            this.qmoFactors = otherKey.qmoFactors;
        }
        if (this.qpoFactors == null && otherKey.qpoFactors != null) {
            this.qpoFactors = otherKey.qpoFactors;
        }

        if (keyMergeStrategy == KeyMergeStrategy.IGNORE_INFO) {
            this.info = null;
        } else if (otherKey.getInfo() != null || this.getInfo() != null) {
            if (this.info == null) {
                this.info = new JSONObject(otherKey.info);
            } else if (otherKey.info != null) {
                if (keyMergeStrategy == KeyMergeStrategy.COLLAPSE_INFO) {
                    JSONObject infoA = this.getInfo();
                    JSONObject infoB = otherKey.getInfo();
                    for (Object obj : infoB.keySet()) {
                        Object infoPartA = infoA.get(obj);
                        Object infoPartB = infoB.get(obj);
                        List<Object> list = new LinkedList<>();
                        if (infoPartA instanceof Collection) {
                            list.addAll((Collection<?>) infoPartA);
                        } else {
                            list.add(infoPartA);
                        }
                        if (infoPartB instanceof Collection) {
                            list.addAll((Collection<?>) infoPartB);
                        } else {
                            list.add(infoPartB);
                        }
                        JSONArray array = new JSONArray();
                        array.addAll(list);
                        infoA.remove(obj);
                        infoA.put(obj, array);
                    }
                    this.setInfo(infoA);
                } else if (keyMergeStrategy == KeyMergeStrategy.APPEND_INFO) {
                    Object combinedInfo = this.info.get("combined_info");
                    if (combinedInfo == null) {
                        combinedInfo = new JSONArray();
                        ((JSONArray) combinedInfo).add(this.info);
                    }
                    ((JSONArray) combinedInfo).add(otherKey.info);
                    this.info = new JSONObject();
                    this.info.put("combined_info", combinedInfo);
                }
            }
        }
        if (otherKey.getSource() != null) {
            if (this.getSource() != null) {
                this.source.addAll(otherKey.getSource());
            } else {
                this.source = new TreeSet<>(otherKey.getSource());
            }
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
                    Object infoPartA = infoA.get(obj);
                    Object infoPartB = infoB.get(obj);
                    List<Object> list = new LinkedList<>();
                    if (infoPartA instanceof Collection) {
                        list.addAll((Collection<?>) infoPartA);
                    } else {
                        list.add(infoPartA);
                    }
                    if (infoPartB instanceof Collection) {
                        list.addAll((Collection<?>) infoPartB);
                    } else {
                        list.add(infoPartB);
                    }
                    JSONArray array = new JSONArray();
                    array.addAll(list);
                    infoA.remove(obj);
                    infoA.put(obj, array);
                }
            }
            key.setInfo(infoA);
        }
        if (key.getSource() != null || this.getSource() != null) {
            Set<String> sources = new TreeSet<>(); //new CopyOnWriteArraySet<>();
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
    protected static List<ClassificationFactor> parseArrayOfFactors(JSONArray array) {
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
        if (rsaKey.getP() != null && rsaKey.getQ() != null) object.put("ordered", ordered);
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
        if (identification != null) object.put("mask", identification);
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
