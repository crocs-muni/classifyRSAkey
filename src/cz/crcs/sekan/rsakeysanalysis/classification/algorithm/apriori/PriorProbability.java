package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori;

import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.util.*;

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

    public static final int BIG_DECIMAL_SCALE = 20;

    public static PriorProbability uniformProbability(List<String> groupNames) {
        PriorProbability priorProbability = new PriorProbability();
        BigDecimal probability = BigDecimal.ONE.divide(BigDecimal.valueOf(groupNames.size()), BIG_DECIMAL_SCALE, BigDecimal.ROUND_HALF_EVEN);
        for (String groupName : groupNames) {
            priorProbability.put(groupName, probability);
        }
        return priorProbability;
    }

    public PriorProbability makeCopy() {
        PriorProbability copy = new PriorProbability();
        for (Map.Entry<String, BigDecimal> entry : entrySet()) {
            copy.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }
}
