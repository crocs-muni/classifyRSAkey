package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori;

import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * @author xnemec1
 * @version 11/24/16.
 */
public class PriorProbability extends TreeMap<String, BigDecimal> {
    public BigDecimal setGroupProbability(String groupName, BigDecimal priorProbability) {
        return put(groupName, priorProbability);
    }

    public BigDecimal getGroupProbability(String groupName) {
        return get(groupName);
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        List<String> sortedGroups = new ArrayList<>(keySet());
        Collections.sort(sortedGroups);
        for (String groupName : sortedGroups) {
            object.put(groupName, getGroupProbability(groupName).toString());
        }
        return object;
    }
}
