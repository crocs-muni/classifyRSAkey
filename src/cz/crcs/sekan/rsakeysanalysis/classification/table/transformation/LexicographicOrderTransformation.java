package cz.crcs.sekan.rsakeysanalysis.classification.table.transformation;

import cz.crcs.sekan.rsakeysanalysis.classification.table.identification.IdentificationGenerator;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.TransformationNotFoundException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongOptionsFormatException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongTransformationFormatException;
import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;
import javafx.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Matus Nemec
 * @version 25/10/2016
 */
public class LexicographicOrderTransformation extends Transformation {

    /**
     * Set of transformations which are applied on the key and ordered lexicographically
     */
    private Set<Pair<Transformation, String>> transformations = new HashSet<>();

    public LexicographicOrderTransformation(RSAKey.PART from, JSONObject options) throws WrongOptionsFormatException {
        super(from, options);
        if (!options.containsKey("transformations") ||
                !(options.get("transformations") instanceof JSONArray)) {
            throw new WrongOptionsFormatException("Transformation Combination has not completely or correct options.");
        }

        JSONArray ts = (JSONArray) options.get("transformations");
        try {
            for (Object tr : ts) {
                JSONObject transformation = (JSONObject) tr;
                transformations.add(new Pair<>(Transformation.createFromIdentificationPart((JSONObject) transformation.get("transformation")), (String)transformation.get("equals")));
            }
        }
        catch (TransformationNotFoundException |WrongTransformationFormatException ex) {
            throw new WrongOptionsFormatException("Cannot create subtransformation in CombinationTransformation.", ex);
        }
    }

    @Override
    public String transform(RSAKey key) {
        List<String> transformationResults = new LinkedList<>();
        for (Pair<Transformation, String> transformation : transformations) {
            transformationResults.add(transformation.getKey().transform(key));
        }
        transformationResults.sort(String.CASE_INSENSITIVE_ORDER);
        return String.join(IdentificationGenerator.TRANSFORMATION_SEPARATOR, transformationResults);
    }
}
