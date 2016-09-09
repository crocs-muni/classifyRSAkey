package cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 20.04.2016
 */
public class WrongTransformationFormatException extends Exception {
    public WrongTransformationFormatException() {
        super();
    }

    public WrongTransformationFormatException(String message) {
        super(message);
    }

    public WrongTransformationFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongTransformationFormatException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
