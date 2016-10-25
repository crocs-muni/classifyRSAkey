package cz.crcs.sekan.rsakeysanalysis.classification.table.identification;

import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.Transformation;
import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 20.04.2016
 */
public class IdentificationGenerator {
    public static final String TRANSFORMATION_SEPARATOR = "|";

    /**
     * Array of transformations
     */
    private List<Transformation> transformations;

    /**
     * Create generator
     * @param transformations list of used transformations
     */
    public IdentificationGenerator(List<Transformation> transformations) {
        this.transformations = transformations;
    }

    /**
     * Generate identification by this generator
     * @param key rsa key
     * @return identification
     */
    public String generationIdentification(RSAKey key) {
        List<String> identifications = transformations.stream()
                .map(transformation -> transformation.transform(key))
                .collect(Collectors.toList());
        return String.join(TRANSFORMATION_SEPARATOR, identifications);
    }
}
