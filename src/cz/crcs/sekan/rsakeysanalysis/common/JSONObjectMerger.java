package cz.crcs.sekan.rsakeysanalysis.common;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xnemec1
 * @version 4/26/17.
 */
public class JSONObjectMerger {
    /**
     *
     * @param objects
     * @return
     */
    public static JSONObject mergeJSONObjects(List<JSONObject> objects) {
        if (objects == null || objects.size() == 0) return null;
        if (objects.size() == 1) return objects.get(0);
        Set<String> keys = new HashSet<>();

        Long count = 0L;

        for (JSONObject object : objects) {
            Set<Object> objectKeys = object.keySet();
            Object duplicities = object.getOrDefault(ClassificationKey.DUPLICITY_COUNT_FIELD, 1L);
            if (duplicities instanceof Number) {
                count += ((Number) duplicities).longValue();
            }
            for (Object o : objectKeys) {
                if (ClassificationKey.DUPLICITY_COUNT_FIELD.equals(o)) {
                    continue;
                }
                if (o != null) keys.add(o.toString());
            }
        }

        JSONObject combinedObject = new JSONObject();
        for (String key : keys) {
            Set<Object> uniqueValues = new HashSet<>();
            boolean allObjectsAreJSONObjects = true;
            for (JSONObject object : objects) {
                if (object.containsKey(key)) {
                    Object value = object.get(key);
                    if (value instanceof JSONArray) {
                        ((JSONArray) value).forEach(uniqueValues::add);
                    } else {
                        uniqueValues.add(value);
                    }
                    if (!(value instanceof JSONObject)) {
                        allObjectsAreJSONObjects = false;
                    }
                }
            }
            Object combinedValue;
            if (allObjectsAreJSONObjects) {
                ArrayList<JSONObject> jsonObjects = new ArrayList<>(uniqueValues.size());
                jsonObjects.addAll(uniqueValues.stream().map(o -> (JSONObject) o).collect(Collectors.toList()));
                combinedValue = mergeJSONObjects(jsonObjects);
            } else {
                combinedValue = new JSONArray();
                ((JSONArray) combinedValue).addAll(uniqueValues);
            }
            combinedObject.put(key, combinedValue);
        }
        combinedObject.put(ClassificationKey.DUPLICITY_COUNT_FIELD, count);
        //System.err.println(String.format("Combining:\n%s\n%s\nResult:\n%s", objects[0], objects[1], combinedObject));
        return combinedObject;
    }
}
