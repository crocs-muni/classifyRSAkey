package cz.crcs.sekan.rsakeysanalysis.classification.key;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;

import java.math.BigInteger;

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

    public static ClassificationKeyStub fromClassificationKey(ClassificationKey key, ClassificationTable table) {
        ClassificationKeyStub stub = new ClassificationKeyStub();
        stub.duplicityCount = key.getCount();
        stub.mask = table.generationIdentification(key);
        return stub;
    }

    public String getMask() {
        return mask;
    }

    public int getDuplicityCount() {
        return duplicityCount;
    }
}
