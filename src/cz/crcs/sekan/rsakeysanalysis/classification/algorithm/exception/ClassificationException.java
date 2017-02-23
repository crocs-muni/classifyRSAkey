package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception;

/**
 * @author xnemec1
 * @version 11/23/16.
 */
public class ClassificationException extends Exception {
    public ClassificationException() {
    }

    public ClassificationException(String message) {
        super(message);
    }

    public ClassificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClassificationException(Throwable cause) {
        super(cause);
    }

    public ClassificationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
