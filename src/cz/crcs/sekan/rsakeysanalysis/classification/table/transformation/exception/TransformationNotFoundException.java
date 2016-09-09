package cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 20.04.2016
 */
public class TransformationNotFoundException extends Exception {
    public TransformationNotFoundException() {
        super();
    }

    public TransformationNotFoundException(String message) {
        super(message);
    }

    public TransformationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformationNotFoundException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
