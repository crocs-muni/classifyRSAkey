package cz.crcs.sekan.rsakeysanalysis.classification.table.makefile.exception;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 20.04.2016
 */
public class WrongMakefileFormatException extends Exception {
    public WrongMakefileFormatException() {
        super();
    }

    public WrongMakefileFormatException(String message) {
        super(message);
    }

    public WrongMakefileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongMakefileFormatException(Throwable cause) {
        super(cause);
    }
}
