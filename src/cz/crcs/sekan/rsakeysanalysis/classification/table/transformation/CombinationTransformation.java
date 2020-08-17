package cz.crcs.sekan.rsakeysanalysis.classification.table.transformation;

import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.TransformationNotFoundException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongOptionsFormatException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongTransformationFormatException;
import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;
import cz.crcs.sekan.rsakeysanalysis.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 27/09/2016
 */
public class CombinationTransformation extends Transformation {
    /**
     * Operators
     */
    public enum OPERATOR {
        AND, OR
    }

    /**
     * Set of transformations
     */
    private Set<Pair<Transformation, String>> transformations = new HashSet<>();

    /**
     * Operator
     */
    private OPERATOR operator;

    public CombinationTransformation(RSAKey.PART from, JSONObject options) throws WrongOptionsFormatException {
        super(from, options);
        if (!options.containsKey("transformations") ||
            !(options.get("transformations") instanceof JSONArray) ||
            !options.containsKey("operator")) {
            throw new WrongOptionsFormatException("Transformation Combination has not completely or correct options.");
        }

        JSONArray ts = (JSONArray) options.get("transformations");
        try {
            for (Object tr : ts) {
                JSONObject transformation = (JSONObject) tr;
                transformations.add(new Pair<>(Transformation.createFromIdentificationPart((JSONObject) transformation.get("transformation")), (String)transformation.get("equals")));
            }
        }
        catch (TransformationNotFoundException|WrongTransformationFormatException ex) {
            throw new WrongOptionsFormatException("Cannot create subtransformation in CombinationTransformation.", ex);
        }

        operator = OPERATOR.valueOf(((String)options.get("operator")).toUpperCase());
        if (operator == null) {
            throw new WrongOptionsFormatException("Wrong operator option in CombinationTransformation.");
        }
    }

    @Override
    public String transform(RSAKey key) {
        for (Pair<Transformation, String> transformation : transformations) {
            boolean res = transformation.getKey().transform(key).equals(transformation.getValue());
            if (!res && operator == OPERATOR.AND) {
                return "0";
            }
            else if (res && operator == OPERATOR.OR) {
                return "1";
            }
        }
        return (operator == OPERATOR.AND ? "1" : "0");
    }
}
