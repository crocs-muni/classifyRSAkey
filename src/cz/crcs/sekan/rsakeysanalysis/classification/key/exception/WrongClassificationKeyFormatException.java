package cz.crcs.sekan.rsakeysanalysis.classification.key.exception;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 20.04.2016
 */
public class WrongClassificationKeyFormatException extends Exception {
    public WrongClassificationKeyFormatException() {
        super();
    }

    public WrongClassificationKeyFormatException(String message) {
        super(message);
    }

    public WrongClassificationKeyFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongClassificationKeyFormatException(Throwable cause) {
        super(cause);
    }
}
