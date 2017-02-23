package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception;

import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.ExpectedRemainderTransformation;

/**
 * @author xnemec1
 * @version 11/23/16.
 */
public class DataSetException extends ClassificationException {
    public DataSetException() {
    }

    public DataSetException(String message) {
        super(message);
    }

    public DataSetException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataSetException(Throwable cause) {
        super(cause);
    }

    public DataSetException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
