package cz.crcs.sekan.rsakeysanalysis.classification.table;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 15.02.2016
 */
public class ClassificationContainer {
    private long numOfRows = 0;
    private long numOfKeys = 0;
    private ClassificationRow row;

    public ClassificationContainer(long numOfDuplicityKeys, ClassificationRow row) {
        numOfRows = 1;
        numOfKeys = numOfDuplicityKeys;
        this.row = row;
    }

    public void add(long numOfDuplicityKeys, ClassificationRow row) {
        numOfRows++;
        numOfKeys += numOfDuplicityKeys;
        this.row = this.row.computeWithSameSource(row);
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
}
