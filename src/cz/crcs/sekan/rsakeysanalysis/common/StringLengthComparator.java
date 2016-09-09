package cz.crcs.sekan.rsakeysanalysis.common;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 07.06.2016
 */
public class StringLengthComparator implements java.util.Comparator<String> {
    public int compare(String s1, String s2) {
        int lengthDiff = s1.length() - s2.length();
        if (lengthDiff != 0) return lengthDiff;
        return s1.compareTo(s2);
    }
}
