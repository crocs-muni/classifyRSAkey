package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.lang.reflect.Array;
import java.util.*;

/**
 * @author xnemec1
 * @version 11/24/16.
 */
public abstract class PriorProbabilityEstimator {
    protected ClassificationTable table;

    protected Map<String, Long> maskToFrequency;

    public PriorProbabilityEstimator(ClassificationTable table) {
        this.table = table.makeCopy();
        maskToFrequency = new TreeMap<>();
    }

    public void addMask(String mask) {
        Long maskCount = maskToFrequency.getOrDefault(mask, 0L);
        maskToFrequency.put(mask, maskCount + 1);
    }

    public abstract PriorProbability computePriorProbability();

    public JSONObject summaryToJSON() {
        JSONObject object = new JSONObject();
        object.put("probability", computePriorProbability().toJSON());
        JSONObject maskFrequencies = new JSONObject();
        List<String> sortedMasks = new ArrayList<>(maskToFrequency.keySet());
        Collections.sort(sortedMasks);
        for (String mask : sortedMasks) {
            maskFrequencies.put(mask, maskToFrequency.get(mask));
        }
        object.put("frequencies", maskFrequencies);
        JSONObject groupMembers = new JSONObject();
        for (String groupName : table.getGroupsNames()) {
            JSONArray sources = new JSONArray();
            for (String source : table.getGroupSources(groupName)) {
                sources.add(source);
            }
            groupMembers.put(groupName, sources);
        }
        object.put("groups", groupMembers);
        return object;
    }
}
