package cz.crcs.sekan.rsakeysanalysis.classification.table;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 07.02.2016
 */
public class ClassificationRow {
    private Map<String, BigDecimal> sources = new TreeMap<>();

    private ClassificationRow() {}

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

    /**
     * Get names of top groups
     * @param number num of values
     * @return set of groups names
     */
    public Set<String> getTopGroups(int number) {
        SortedSet<Map.Entry<String, BigDecimal>> set = new TreeSet<>(new Comparator<Map.Entry<String, BigDecimal>>() {
            @Override
            public int compare(Map.Entry<String, BigDecimal> e1,
                               Map.Entry<String, BigDecimal> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        });
        set.addAll(sources.entrySet());
        Set<String> groups = new HashSet<>();
        for (Map.Entry<String, BigDecimal> val : set) {
            if (number <= 0) break;
            groups.add(val.getKey());
            number--;
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
        result.normalize();
        return result;
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
}
