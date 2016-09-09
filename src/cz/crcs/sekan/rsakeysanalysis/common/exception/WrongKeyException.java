package cz.crcs.sekan.rsakeysanalysis.common.exception;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 28.10.2015
 */
public class WrongKeyException extends Exception {
    public WrongKeyException() {
        super();
    }

    public WrongKeyException(String message) {
        super(message);
    }

    public WrongKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongKeyException(Throwable cause) {
        super(cause);
    }
}
