package cz.crcs.sekan.rsakeysanalysis.classification.table;

import org.json.simple.JSONObject;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 15.02.2016
 */
public class ClassificationContainer {
    private long numOfUniqueKeys = 0;
    private long numOfAllKeys = 0;
    private ClassificationRow row;

    public ClassificationContainer(long numOfDuplicityKeys, ClassificationRow row) {
        numOfUniqueKeys = 1;
        numOfAllKeys = numOfDuplicityKeys;
        this.row = row;
    }

    public void add(long numOfDuplicityKeys, ClassificationRow row) {
        numOfUniqueKeys++;
        numOfAllKeys += numOfDuplicityKeys;
        this.row = this.row.computeWithSameSource(row);
    }

    public long getNumOfUniqueKeys() {
        return numOfUniqueKeys;
    }

    public long getNumOfAllKeys() {
        return numOfAllKeys;
    }

    public ClassificationRow getRow() {
        return row;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("keyCount", getNumOfAllKeys());
        jsonObject.put("uniqueKeyCount", getNumOfUniqueKeys());
        jsonObject.put("classification", row.toJSON());
        return jsonObject;
    }
}
