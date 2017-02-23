package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import org.json.simple.JSONObject;

/**
 * @author xnemec1
 * @version 11/28/16.
 */
public class JsonDataSetFormatter implements DataSetFormatter {
    @Override
    public String classifiedKeyToLine(ClassificationKey key, ClassificationContainer container) {
        JSONObject jsonKey = key.toJSON();
        if (container != null) jsonKey.put("classification", container.toJSON());
        return jsonKey.toString();
    }
}
