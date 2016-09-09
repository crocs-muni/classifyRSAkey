package cz.crcs.sekan.rsakeysanalysis.classification.table.transformation;

import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.TransformationNotFoundException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongOptionsFormatException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongTransformationFormatException;
import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;
import org.json.simple.JSONObject;

import java.lang.reflect.Constructor;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 18.04.2016
 */
public abstract class Transformation {
    /**
     * Part of rsa key which is transformed to identification part
     */
    protected RSAKey.PART from;

    /**
     * Json object of transformation's options
     */
    protected JSONObject options;

    /**
     * JSONObject contains options for transformation
     * @param from part of rsa key which is transformed to identification part
     * @param options JSONObject contains options for transformation
     * @throws WrongOptionsFormatException
     */
    public Transformation(RSAKey.PART from, JSONObject options) throws WrongOptionsFormatException {
        this.from = from;
        this.options = options;
    }

    protected Object getRequiredOption(String option) throws WrongOptionsFormatException {
        if (!options.containsKey(option)) {
            throw new WrongOptionsFormatException("Options for " + this.getClass().getName() + " does not contains parameter \"" + option + "\".");
        }
        return options.get(option);
    }

    /**
     * Transform part of key to identification part
     * @param key rsa key
     * @return identification part
     */
    public abstract String transform(RSAKey key);

    /**
     * Create transformation object from identification part json object
     * @param identificationPart json object
     * @return transformation
     * @throws WrongTransformationFormatException
     * @throws TransformationNotFoundException
     */
    public static Transformation createFromIdentificationPart(JSONObject identificationPart) throws WrongTransformationFormatException, TransformationNotFoundException {
        if (!identificationPart.containsKey("transformationId")) {
            throw new WrongTransformationFormatException("Transformation does not contain parameter 'transformationId'");
        }
        if (!identificationPart.containsKey("transform")) {
            throw new WrongTransformationFormatException("Transformation does not contain parameter 'transform'");
        }
        if (!identificationPart.containsKey("options")) {
            throw new WrongTransformationFormatException("Transformation does not contain parameter 'options'");
        }

        String transformationId = (String)identificationPart.get("transformationId");
        String transform = ((String)identificationPart.get("transform")).toLowerCase();

        if (!transformationId.matches("[a-zA-Z0-9]+")) {
            throw new WrongTransformationFormatException("Transformation parameter 'transformationId' is empty or contains not allowed characters");
        }

        RSAKey.PART from;
        switch (transform) {
            case "n": from = RSAKey.PART.N; break;
            case "e": from = RSAKey.PART.E; break;
            case "d": from = RSAKey.PART.D; break;
            case "p": from = RSAKey.PART.P; break;
            case "q": from = RSAKey.PART.Q; break;
            case "phi": from = RSAKey.PART.PHI; break;
            case "nblen": from = RSAKey.PART.NBLEN; break;
            case "pblen": from = RSAKey.PART.PBLEN; break;
            case "qblen": from = RSAKey.PART.QBLEN; break;
            default: throw new WrongTransformationFormatException("Transformation parameter 'transform' is not one of {N,E,D,P,Q,PHI,NBLEN,PBLEN,QBLEN}");
        }

        try {
            Class<?> transformationClass = Class.forName(Transformation.class.getPackage().getName() + "." + transformationId + "Transformation");
            Constructor<?> constructor = transformationClass.getConstructor(RSAKey.PART.class, JSONObject.class);
            return (Transformation)constructor.newInstance(from, identificationPart.get("options"));
        }
        catch (Exception ex) {
            throw new TransformationNotFoundException("Cannot create transformation with id '" + transformationId + "'.", ex);
        }
    }
}
