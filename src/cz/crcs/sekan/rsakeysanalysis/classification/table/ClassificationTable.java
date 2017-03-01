package cz.crcs.sekan.rsakeysanalysis.classification.table;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbability;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.table.identification.IdentificationGenerator;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import javafx.util.Pair;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 07.02.2016
 */
public class ClassificationTable {
    private Map<String, ClassificationRow> table = new TreeMap<>();
    private Map<String, Set<String>> groups = new TreeMap<>();
    private PriorProbability priorProbability = new PriorProbability();

    private IdentificationGenerator identificationGenerator;

    public ClassificationTable(Map<Set<String>, Map<String, Long>> tableGrouped, IdentificationGenerator identificationGenerator, Map<Set<String>, BigDecimal> groupWeights) {
        this.identificationGenerator = identificationGenerator;
        Map<String, Map<String, Double>> normalized = new TreeMap<>();

        boolean tenOrMoreGroups = tableGrouped.keySet().size() >= 10;
        int i = 0;
        for (Set<String> group : tableGrouped.keySet()) {
            String groupName = "Group " + (tenOrMoreGroups && i < 9 ? " " : "") + (i+1);
            groups.put(groupName, group);
            i++;

            long sum = tableGrouped.get(group).values().stream().mapToLong(Long::longValue).sum();
            Map<String, Double> identifications = new TreeMap<>();
            for (Map.Entry<String, Long> entry : tableGrouped.get(group).entrySet()) {
                identifications.put(entry.getKey(), Double.valueOf(entry.getValue()) / sum);
            }
            normalized.put(groupName, identifications);
            priorProbability.put(groupName, groupWeights.get(group));
        }

        Set<String> allIdentifications = new TreeSet<>();
        for (Map<String, Long> map : tableGrouped.values()) {
            allIdentifications.addAll(map.keySet());
        }

        for (String identification : allIdentifications) {
            Map<String, BigDecimal> row = new TreeMap<>();
            for (String groupName : groups.keySet()) {
                Double val = normalized.get(groupName).get(identification);
                if (val != null) {
                    row.put(groupName, BigDecimal.valueOf(val));
                }
            }
            ClassificationRow classificationRow = new ClassificationRow(row);
            table.put(identification, classificationRow);
        }
    }

    public void applyPriorProbability(PriorProbability priorProbability) {
        applyPriorProbability(priorProbability, true);
    }

    public void applyPriorProbability(PriorProbability priorProbability, boolean normalize) {
        if (priorProbability != null) {
            // use the default user defined prior probabilities from the table, otherwise replace
            this.priorProbability = priorProbability;
        }

        for (String identification : table.keySet()) {
            ClassificationRow row = table.get(identification);
            row.applyPriorProbabilities(this.priorProbability, normalize);
        }
    }

    public PriorProbability getPriorProbability() {
        return priorProbability;
    }

    public ClassificationRow classifyKey(ClassificationKey key) {
        return table.get(generationIdentification(key));
    }

    public String generationIdentification(ClassificationKey key) {
        return identificationGenerator.generationIdentification(key.getRsaKey());
    }

    public ClassificationRow classifyIdentification(String identification) {
        return table.get(identification);
    }

    /**
     * Get sources in group
     *
     * @param groupName name of group
     * @return set of source names
     */
    public Set<String> getGroupSources(String groupName) {
        return groups.get(groupName);
    }

    /**
     * Get names of groups
     *
     * @return set of groups names
     */
    public Set<String> getGroupsNames() {
        return groups.keySet();
    }

    public Map<String, ClassificationRow> getTable() {
        return table;
    }

    public List<ClassificationRow> getClassificationRowsForGroup(String groupName) {
        ArrayList<ClassificationRow> rows = new ArrayList<>();
        for (ClassificationRow row : table.values()) {
            if (row.getSource(groupName) != null) {
                rows.add(row);
            }
        }
        return rows;
    }

    public void exportToCsvFormat(String outFileName) throws IOException {
        try (ExtendedWriter writer = new ExtendedWriter(outFileName)) {
            writer.writeln("Group name,Group sources");
            for (String groupName : getGroupsNames()) {
                writer.writeln(groupName + "," + String.join(",", getGroupSources(groupName)));
            }
            writer.newLine();

            DecimalFormat formatter = new DecimalFormat("#0.0000");
            writer.writeln("Bits," + String.join(",", getGroupsNames()));
            for (Map.Entry<String, ClassificationRow> entry : table.entrySet()) {
                writer.write(entry.getKey());
                for (String group : getGroupsNames()) {
                    BigDecimal val = entry.getValue().getSource(group);
                    if (val == null) {
                        writer.write(",-");
                    }
                    else {
                        writer.write("," + formatter.format(val.multiply(BigDecimal.valueOf(100.0)).doubleValue()));
                    }
                }
                writer.newLine();
            }
        }
    }

    private ClassificationTable() {

    }

    public ClassificationTable makeCopy() {
        // normalizations are tricky, make deep copy of the classification rows, other stuff should not change
        Map<String, ClassificationRow> newTable = new HashMap<>();
        for (Map.Entry<String, ClassificationRow> entry : table.entrySet()) {
            newTable.put(entry.getKey(), entry.getValue().deepCopy());
        }
        ClassificationTable copyTable = new ClassificationTable();
        copyTable.table = newTable;
        copyTable.groups = groups;
        copyTable.identificationGenerator = identificationGenerator;
        copyTable.priorProbability = priorProbability;
        return copyTable;
    }

    public List<String> getMasks() {
        return new ArrayList<>(getTable().keySet());
    }
}
