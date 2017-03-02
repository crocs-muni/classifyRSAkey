package cz.crcs.sekan.rsakeysanalysis.classification.algorithm;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.PriorProbability;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.RawTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.TransformationNotFoundException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongTransformationFormatException;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * @author xnemec1
 * @version 3/2/17.
 */
public class ClassificationConfiguration {
    public static final String BATCH_TYPE_SWITCH = "-b";
    public static final String PRIOR_TYPE_SWITCH = "-p";
    public static final String EXPORT_TYPE_SWITCH = "-e";
    public static final String MEMORY_TYPE_SWITCH = "-t";
    public static final String KEY_COUNT_SWITCH = "-c";
    public static final String RNG_SEED_SWITCH = "-s";
    public static final String PRIOR_PROBABILITY_SWITCH = "-a";
    public int consumedArguments;

    // classification + success
    public Classification.BatchType batchType = Classification.BatchType.SOURCE;
    public Classification.PriorType priorType = Classification.PriorType.ESTIMATE;
    public Classification.ExportType exportType = Classification.ExportType.NONE;
    public Classification.MemoryType memoryType = Classification.MemoryType.DISK;
    public ClassificationTable classificationTable;
    public String outputFolderPath;

    // success
    public int keyCount;
    public Long rngSeed;
    public PriorProbability priorProbability;

    public SecureRandom configureRandom() throws NoSuchAlgorithmException {
        if (rngSeed == null) {
            SecureRandom seedingRandom = SecureRandom.getInstance("SHA1PRNG");
            rngSeed = seedingRandom.nextLong();
        }
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(rngSeed);
        return random;
    }

    public static ClassificationConfiguration fromCommandLineOptions(String[] args, int offset,
                                                                 String tableFilePath, String outputFolderPath)
            throws IOException, ParseException, WrongTransformationFormatException, TransformationNotFoundException, DataSetException {

        ClassificationConfiguration returnObject = new ClassificationConfiguration();

        if ((args.length - offset) % 2 != 0) {
            //throw new IllegalArgumentException("Bad number of arguments, some switch might be missing an option");
        }

        returnObject.consumedArguments = offset;

        RawTable table = RawTable.load(tableFilePath);
        returnObject.classificationTable = table.computeClassificationTable();

        for (; returnObject.consumedArguments < args.length; returnObject.consumedArguments++) {
            switch (args[returnObject.consumedArguments]) {
                case BATCH_TYPE_SWITCH:
                    returnObject.batchType = Classification.BatchType.valueOf(args[++returnObject.consumedArguments].toUpperCase());
                    break;
                case PRIOR_TYPE_SWITCH:
                    returnObject.priorType = Classification.PriorType.valueOf(args[++returnObject.consumedArguments].toUpperCase());
                    break;
                case EXPORT_TYPE_SWITCH:
                    returnObject.exportType = Classification.ExportType.valueOf(args[++returnObject.consumedArguments].toUpperCase());
                    break;
                case MEMORY_TYPE_SWITCH:
                    returnObject.memoryType = Classification.MemoryType.valueOf(args[++returnObject.consumedArguments].toUpperCase());
                    break;
                case KEY_COUNT_SWITCH:
                    returnObject.keyCount = Integer.valueOf(args[++returnObject.consumedArguments]);
                    break;
                case RNG_SEED_SWITCH:
                    returnObject.rngSeed = Long.valueOf(args[++returnObject.consumedArguments]);
                    break;
                case PRIOR_PROBABILITY_SWITCH:
                    // TODO prior probability configuration the same as for classification
                    returnObject.priorProbability = PriorProbability.uniformProbability(
                            new ArrayList<>(returnObject.classificationTable.getGroupsNames()));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid option for classification: " + args[returnObject.consumedArguments]);
            }
        }

        if (outputFolderPath != null) {
            //Create folder for results if does not exist
            File folderFile = new File(outputFolderPath);
            if (!folderFile.exists()) {
                if (!folderFile.mkdirs()) {
                    throw new IllegalArgumentException("Cannot create folder.");
                }
            }
        }
        returnObject.outputFolderPath = outputFolderPath;
        return returnObject;
    }

    public ClassificationConfiguration deepCopy() {
        ClassificationConfiguration copy = new ClassificationConfiguration();
        copy.consumedArguments = consumedArguments;
        copy.batchType = batchType;
        copy.priorType = priorType;
        copy.exportType = exportType;
        copy.memoryType = memoryType;
        copy.outputFolderPath = outputFolderPath;
        copy.keyCount = keyCount;
        copy.rngSeed = rngSeed;
        copy.classificationTable = classificationTable.makeCopy();
        copy.priorProbability = priorProbability.makeCopy();
        return copy;
    }
}
