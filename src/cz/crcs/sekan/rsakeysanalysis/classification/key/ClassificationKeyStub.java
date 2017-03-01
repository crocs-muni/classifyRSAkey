package cz.crcs.sekan.rsakeysanalysis.classification.key;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;

/**
 * @author xnemec1
 * @version 11/22/16.
 */
public class ClassificationKeyStub {

    /**
     * The classification identification/mask
     */
    private String mask;

    /**
     * The count of keys with the same id
     */
    private int duplicityCount;

    /**
     * Usd for classification success test - the real group which generated this key.
     */
    private String realSource;

    public static ClassificationKeyStub fromClassificationKey(ClassificationKey key, ClassificationTable table) {
        ClassificationKeyStub stub = new ClassificationKeyStub();
        stub.duplicityCount = key.getCount();
        if (key.getRealSource() != null && key.getIdentification() != null) {
            // this is a test key for the purpose of classification success
            stub.mask = key.getIdentification();
        } else {
            stub.mask = table.generationIdentification(key);
        }
        stub.realSource = key.getRealSource();
        return stub;
    }

    public String getMask() {
        return mask;
    }

    public int getDuplicityCount() {
        return duplicityCount;
    }

    public String getRealSource() {
        return realSource;
    }
}
