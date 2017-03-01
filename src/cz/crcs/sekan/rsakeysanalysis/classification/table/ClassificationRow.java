package cz.crcs.sekan.rsakeysanalysis.classification.table;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbability;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;

/**
 * TODO use Group ID to Group name mapping and lists/arrays
 *
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 07.02.2016
 */
public class ClassificationRow {
    private Map<String, BigDecimal> sources = new TreeMap<>();

    private ClassificationRow() {}

    public ClassificationRow(String[] sources) {
        for (String source : sources) {
            this.sources.put(source, BigDecimal.ZERO);
        }
    }

    public ClassificationRow(String[] sources, String[] values) {
        if (sources.length != values.length) throw new IllegalArgumentException("Arguments sources and values in ClassificationRow constructor have not same size.");

        for (int i = 0; i< sources.length; i++) {
            if (!values[i].equals("-"))this.sources.put(sources[i], BigDecimal.valueOf(Double.parseDouble(values[i])/100));
        }
    }

    public ClassificationRow(Map<String, BigDecimal> sources) {
        this.sources = sources;
    }

    /**
     * Get classification value of source from classification row.
     *
     * @param source name of group/source
     * @return classification value
     */
    public BigDecimal getSource(String source) {
        return sources.get(source);
    }

    public static final int ALL_GROUPS = -1;

    /**
     * Get names of top groups
     * @param number num of values, or ALL_GROUPS for all
     * @return set of groups names
     */
    public List<String> getTopGroups(int number) {
        SortedSet<Map.Entry<String, BigDecimal>> set = new TreeSet<>((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        set.addAll(sources.entrySet());
        List<String> groups = new ArrayList<>();
        int groupsLeft = number;
        for (Map.Entry<String, BigDecimal> val : set) {
            if (number != ALL_GROUPS && groupsLeft <= 0) break;
            groups.add(val.getKey());
            groupsLeft--;
        }
        return groups;
    }

    /**
     * Get position of group in sorted classification row
     * @param group name
     * @return position|-1
     */
    public int getGroupPosition(String group) {
        SortedSet<Map.Entry<String, BigDecimal>> set = new TreeSet<>(new Comparator<Map.Entry<String, BigDecimal>>() {
            @Override
            public int compare(Map.Entry<String, BigDecimal> e1,
                               Map.Entry<String, BigDecimal> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        });
        set.addAll(sources.entrySet());

        int position = 1;
        for (Map.Entry<String, BigDecimal> val : set) {
            if (val.getKey().equals(group)) {
                break;
            }
            position++;
        }
        if (position > set.size()) return -1;
        return position;
    }

    /**
     * Get classification values of row.
     *
     * @return map fo values
     */
    public Map<String, BigDecimal> getValues() {
        return sources;
    }

    /**
     * Get max value in row.
     *
     * @return name of group and max value
     */
    public Map.Entry<String, BigDecimal> getMaxValue() {
        Map.Entry<String, BigDecimal> max = null;
        for (Map.Entry<String, BigDecimal> entry : sources.entrySet()) {
            if (max == null || max.getValue().compareTo(entry.getValue()) == -1) {
                max = entry;
            }
        }

        return max;
    }

    /**
     * Compute two classification row and return new computed row.
     *
     * @param otherRow other classification row to compute.
     * @return new computed classification row
     */
    public ClassificationRow computeWithSameSource(ClassificationRow otherRow) {
        ClassificationRow result = new ClassificationRow();
        Set<String> allSources = new HashSet<>();
        allSources.addAll(sources.keySet());
        allSources.addAll(otherRow.sources.keySet());

        for (String source : allSources) {
            BigDecimal valueThis = sources.get(source);
            BigDecimal valueOther = otherRow.sources.get(source);

            if (valueThis != null && valueOther != null) {
                result.sources.put(source, valueThis.multiply(valueOther));
            }
        }
        result.normalize();
        return result;
    }

    /**
     * Compute two classification row and return new computed row.
     *
     * @param otherRow other classification row to compute.
     * @return new computed classification row
     */
    public ClassificationRow computeWithNotSameSource(ClassificationRow otherRow) {
        ClassificationRow result = sumRowsNoNormalize(otherRow);
        result.normalize();
        return result;
    }

    public ClassificationRow sumRowsNoNormalize(ClassificationRow otherRow) {
        ClassificationRow result = new ClassificationRow();
        Set<String> allSources = new HashSet<>();
        allSources.addAll(sources.keySet());
        allSources.addAll(otherRow.sources.keySet());

        for (String source : allSources) {
            BigDecimal valueThis = sources.get(source);
            BigDecimal valueOther = otherRow.sources.get(source);

            if (valueThis != null || valueOther != null) {
                BigDecimal val = BigDecimal.ZERO;
                if (valueThis != null) {
                    val = val.add(valueThis);
                }
                if (valueOther != null) {
                    val= val.add(valueOther);
                }
                result.sources.put(source, val);
            }
        }
        return result;
    }

    /**
     * Turns positive results of classification into zeros and negative (zeros) into ones
     * TODO Requires the map to have all sources! TODO refactor the class :(
     * @return
     */
    public ClassificationRow switchToNegative(List<String> allGroups) {
        Map<String, BigDecimal> resultSources = new TreeMap<>();
        for (String group : allGroups) {
            resultSources.put(group, BigDecimal.ZERO.equals(sources.getOrDefault(group, BigDecimal.ZERO)) ? BigDecimal.ONE : BigDecimal.ZERO);
        }
        return new ClassificationRow(resultSources);
    }

    public ClassificationRow sumRowsNegativeResults(ClassificationRow otherRow, List<String> allGroups) {
        // assuming this has already been switched to negative
        return otherRow.switchToNegative(allGroups).sumRowsNoNormalize(this);
    }

    public ClassificationRow multipleByConstant(Long constant) {
        Map<String, BigDecimal> resultSources = new TreeMap<>();
        for (String key : sources.keySet()) {
            resultSources.put(key, sources.get(key).multiply(BigDecimal.valueOf(constant)));
        }
        return new ClassificationRow(resultSources);
    }

    public static String groupNamesToFormattedString(List<String> groupNamesOrdered, String separator) {
        StringBuilder builder = new StringBuilder();
        int toLast = groupNamesOrdered.size();
        for (String groupName : groupNamesOrdered) {
            builder.append(groupName);
            if (--toLast != 0) builder.append(separator);
        }
        return builder.toString();
    }

    public String toFormattedString(List<String> groupNamesOrdered, DecimalFormat formatter, String separator) {
        StringBuilder builder = new StringBuilder();
        int toLast = groupNamesOrdered.size();
        for (String groupName : groupNamesOrdered) {
            builder.append(formatter.format(sources.get(groupName)));
            if (--toLast != 0) builder.append(separator);
        }
        return builder.toString();
    }

    public String toString() {
        String tmp= "";
        boolean first = true;
        for (Map.Entry<String, BigDecimal> entry : sources.entrySet()) {
            if (!first) {
                tmp += ", ";
            }
            else first = false;
            tmp += entry.getKey() + " => " + (entry.getValue().doubleValue() * 100);
        }
        return tmp;
    }

    public JSONObject toJSON() {
        JSONObject row = new JSONObject();
        for (Map.Entry<String, BigDecimal> entry : sources.entrySet()) {
            row.put(entry.getKey(), entry.getValue());
        }
        return row;
    }

    /**
     * Normalize values to interval [0,1] and sum of all values set to 1.
     */
    public void normalize() {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal val : sources.values()) {
            sum = sum.add(val);
        }
        if (sum.compareTo(BigDecimal.ZERO) == 0) return;
        for (Map.Entry<String, BigDecimal> entry : sources.entrySet()) {
            entry.setValue((entry.getValue().divide(sum, 20, BigDecimal.ROUND_CEILING)));
        }
    }

    public ClassificationRow normalizedCopy() {
        ClassificationRow copy = deepCopy();

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal val : sources.values()) {
            sum = sum.add(val);
        }
        if (!BigDecimal.ZERO.equals(sum)) {
            for (Map.Entry<String, BigDecimal> entry : copy.sources.entrySet()) {
                entry.setValue((entry.getValue().divide(sum, 20, BigDecimal.ROUND_CEILING)));
            }
        }
        return copy;
    }

    public void applyPriorProbabilities(PriorProbability priorProbability) {
        applyPriorProbabilities(priorProbability, true);
    }

    public void applyPriorProbabilities(PriorProbability priorProbability, boolean normalize) {
        if (!priorProbability.keySet().containsAll(sources.keySet())) {
            throw new IllegalArgumentException("Not all probabilities are defined");
        }
        for (String group : sources.keySet()) {
            sources.replace(group, sources.get(group).multiply(priorProbability.get(group)));
        }
        if (normalize) normalize();
    }

    public ClassificationRow deepCopy() {
        ClassificationRow copy = new ClassificationRow();
        copy.sources = new TreeMap<>();
        for (Map.Entry<String, BigDecimal> entry : sources.entrySet()) {
            copy.sources.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }
}
