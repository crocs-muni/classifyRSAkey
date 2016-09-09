package cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 20.04.2016
 */
public class WrongOptionsFormatException extends Exception {
    public WrongOptionsFormatException() {
        super();
    }

    public WrongOptionsFormatException(String message) {
        super(message);
    }

    public WrongOptionsFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongOptionsFormatException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
