package cz.crcs.sekan.rsakeysanalysis.classification.table;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 15.02.2016
 */
public class ClassificationContainer {
    private long numOfRows = 0;
    private long numOfKeys = 0;
    private ClassificationRow row;
    private List<ClassificationKey> keys; // necessary, if we want to reconstruct the original dataset

    public ClassificationContainer(long numOfDuplicityKeys, ClassificationRow row) {
        numOfRows = 1;
        numOfKeys = numOfDuplicityKeys;
        this.row = row;
        this.keys = new LinkedList<>();
    }

    public ClassificationContainer(long numOfDuplicityKeys, ClassificationRow row, ClassificationKey key) {
        numOfRows = 1;
        numOfKeys = numOfDuplicityKeys;
        this.row = row;
        this.keys = new LinkedList<>();
        keys.add(key);
    }

    public void add(long numOfDuplicityKeys, ClassificationRow row) {
        numOfRows++;
        numOfKeys += numOfDuplicityKeys;
        this.row = this.row.computeWithSameSource(row);
    }

    public void add(long numOfDuplicityKeys, ClassificationRow row, ClassificationKey key) {
        numOfRows++;
        numOfKeys += numOfDuplicityKeys;
        this.row = this.row.computeWithSameSource(row);
        keys.add(key);
    }

    public void add(long numOfDuplicityKeys, ClassificationRow row, List<ClassificationKey> keys) {
        numOfRows++;
        numOfKeys += numOfDuplicityKeys;
        this.row = this.row.computeWithSameSource(row);
        this.keys.addAll(keys);
    }

    public long getNumOfRows() {
        return numOfRows;
    }

    public long getNumOfKeys() {
        return numOfKeys;
    }

    public ClassificationRow getRow() {
        return row;
    }

    public List<ClassificationKey> getKeys() {
        return keys;
    }
}
