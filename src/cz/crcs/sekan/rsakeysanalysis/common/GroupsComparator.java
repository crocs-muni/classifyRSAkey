package cz.crcs.sekan.rsakeysanalysis.common;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 17.06.2016
 */
public class GroupsComparator implements Comparator<Set<String>> {
    private ArrayList<String> groupRepresentant;

    public GroupsComparator() {
        this.groupRepresentant = null;
    }

    public GroupsComparator(ArrayList<String> groupRepresentant) {
        this.groupRepresentant = groupRepresentant;
    }

    public int compare(Set<String> a, Set<String> b) {
        int ret = 0;
        if (groupRepresentant == null) {
            ret = a.size() - b.size();
        }
        else {
            int firstIndex = -1, secondIndex = -1;
            for (int i = 0; i < groupRepresentant.size(); i++) {
                if (a.contains(groupRepresentant.get(i))) firstIndex = i;
                if (b.contains(groupRepresentant.get(i))) secondIndex = i;
            }
            ret = firstIndex - secondIndex;
        }
        if (ret != 0) return ret;
        return a.toString().compareTo(b.toString());
    }
}
