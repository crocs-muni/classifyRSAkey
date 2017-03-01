package cz.crcs.sekan.rsakeysanalysis.classification.tests.util;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author xnemec1
 * @version 2/27/17.
 */
public class ClassificationGroupSimulator {
    private SampleGenerator<String> maskGenerator;
    private String groupName;

    private ClassificationGroupSimulator(List<String> masks, List<BigDecimal> probabilities, String groupName) {
        maskGenerator = new SampleGenerator<>(masks, probabilities);
        // System.out.println(groupName + " probabilities: " + probabilities);
        this.groupName = groupName;
    }

    public static ClassificationGroupSimulator fromClassificationTable(ClassificationTable table, String groupName) {
        List<String> masks = new ArrayList<>();
        List<BigDecimal> probabilities = new ArrayList<>();

        Map<String, ClassificationRow> rawTable = table.getTable();
        for (String mask : rawTable.keySet()) {
            masks.add(mask);
            ClassificationRow row = rawTable.get(mask);
            BigDecimal probability = row.getValues().getOrDefault(groupName, BigDecimal.ZERO);
            probabilities.add(probability);
        }

        return new ClassificationGroupSimulator(masks, probabilities, groupName);
    }

    public String getMaskFromRandom(BigDecimal random) {
        return maskGenerator.getRandomSample(random);
    }

    public String getGroupName() {
        return groupName;
    }
}
