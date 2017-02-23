package cz.crcs.sekan.rsakeysanalysis.common;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Bidirectional map, where the Key might be associated with multiple Values, but Values added together must be under the same key.
 * N:N relationship is fixed such that the Values which should be associated when adding get a new key.
 *
 * @author xnemec1
 * @version 11/22/16.
 */
public class BidirectionalMap<Key, Value> {
    private Map<Value, Key> valueToKey;
    private Map<Key, Set<Value>> keyToValues;

    public BidirectionalMap() {
        valueToKey = new HashMap<Value, Key>();
        keyToValues = new HashMap<Key, Set<Value>>();
    }

    public List<Key> getKeys() {
        return new ArrayList<Key>(keyToValues.keySet());
    }

    public List<Value> getValues() {
        return new ArrayList<Value>(valueToKey.keySet());
    }

    public List<Value> getValuesByKey(Key key) {
        return keyToValues.containsKey(key) ? new ArrayList<Value>(keyToValues.get(key)) : null;
    }

    public Key getKeyByValue(Value value) {
        return valueToKey.get(value);
    }

    public void joinBatchesUnderNewKey(List<Key> groupKeys, Key newGroupKey) {
        Set<Value> batchedValues = new HashSet<>();

        for (Key batchId : groupKeys) {
            Set<Value> values = keyToValues.get(batchId);
            if (values == null) continue;
            batchedValues.addAll(values);
            keyToValues.remove(batchId);
        }

        keyToValues.put(newGroupKey, batchedValues);

        for (Value value : batchedValues) {
            valueToKey.replace(value, newGroupKey);
        }
    }

    /**
     *
     * @param values
     * @param preferredNewKey
     * @return list of existing and new keys which were grouped under the preferredNewKey, if only one element
     * (preferredNewKey) is present, the value was added under the new key and no joining was done
     */
    public List<Key> placeValuesUnderSameKey(List<Value> values, Key preferredNewKey) {
        List<Key> targetKeys = new ArrayList<>();
        List<Value> missingValues = new ArrayList<>();

        for (Value value : values) {
            Key identifiedKey = valueToKey.get(value);
            if (identifiedKey == null) {
                missingValues.add(value);
                continue;
            }

            targetKeys.add(identifiedKey);
        }

        Key key = preferredNewKey;

        if (!targetKeys.isEmpty()) {
            if (targetKeys.size() == 1) {
                key = targetKeys.get(0);
            } else {
                joinBatchesUnderNewKey(targetKeys, key);
            }
        }

        if (targetKeys.isEmpty()) {
            keyToValues.put(key, new CopyOnWriteArraySet<>(missingValues));
        } else {
            keyToValues.get(key).addAll(missingValues);
        }

        for (Value value : missingValues) {
            valueToKey.put(value, key);
        }

        //targetKeys.add(key);

        return targetKeys;
    }

}
