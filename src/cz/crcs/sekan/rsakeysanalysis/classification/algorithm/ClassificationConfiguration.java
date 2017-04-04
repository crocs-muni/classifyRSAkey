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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author xnemec1
 * @version 3/2/17.
 */
public class ClassificationConfiguration {
    public static final String BATCH_TYPE_SWITCH = "-b";
    public static final String PRIOR_TYPE_SWITCH = "-p";
    public static final String EXPORT_TYPE_SWITCH = "-e";
    public static final String MEMORY_TYPE_SWITCH = "-m";
    public static final String KEY_COUNT_SWITCH = "-c";
    public static final String RNG_SEED_SWITCH = "-s";
    public static final String PRIOR_PROBABILITY_SWITCH = "-a";
    public static final String CLASSIFICATION_TABLE_SWITCH = "-t";
    public static final String OUTPUT_SWITCH = "-o";
    public static final String TEMP_SWITCH = "-tmp";
    public static final String INPUTS_SWITCH = "-i";
    public static final String PRINT_PROGRESS_SWITCH = "-pp";
    public static final String SUPPRESS_PROGRESS_SWITCH = "-sp";
    public static final String ONLY_PRIOR_SWITCH_SWITCH = "-op";

    private static final List<String> allowedSwitches = Arrays.asList(BATCH_TYPE_SWITCH, PRIOR_TYPE_SWITCH,
            EXPORT_TYPE_SWITCH, MEMORY_TYPE_SWITCH, KEY_COUNT_SWITCH, RNG_SEED_SWITCH, PRIOR_PROBABILITY_SWITCH,
            CLASSIFICATION_TABLE_SWITCH, OUTPUT_SWITCH, TEMP_SWITCH, INPUTS_SWITCH, ONLY_PRIOR_SWITCH_SWITCH);

    public int consumedArguments;

    // classification + success
    public Classification.BatchType batchType = Classification.BatchType.SOURCE;
    public Classification.PriorType priorType = Classification.PriorType.ESTIMATE;
    public Classification.ExportType exportType = Classification.ExportType.NONE;
    public Classification.MemoryType memoryType = Classification.MemoryType.DISK;
    public ClassificationTable classificationTable;
    public String outputFolderPath;
    public String tempFolderPath;
    public boolean makeOutputs = true;
    public boolean onlyPriorProbability = false;
    public List<String> inputPaths;

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

    public static ClassificationConfiguration fromCommandLineOptions(String[] args, int offset)
            throws IOException, ParseException, WrongTransformationFormatException, TransformationNotFoundException, DataSetException {

        ClassificationConfiguration returnObject = new ClassificationConfiguration();

        returnObject.consumedArguments = offset;

        boolean inputsBeingEnumerated = false;
        returnObject.inputPaths = new ArrayList<>();

        for (; returnObject.consumedArguments < args.length; returnObject.consumedArguments++) {
            String nextArgument = args[returnObject.consumedArguments];

            if (allowedSwitches.contains(nextArgument)) {
                inputsBeingEnumerated = false;
            } else {
                if (inputsBeingEnumerated) {
                    returnObject.inputPaths.add(nextArgument);
                    continue;
                } else {
                    throw new IllegalArgumentException("Unexpected option for classification: " + nextArgument);
                }
            }

            switch (nextArgument) {
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
                case CLASSIFICATION_TABLE_SWITCH:
                    String tableFilePath = args[++returnObject.consumedArguments];
                    RawTable table = RawTable.load(tableFilePath);
                    returnObject.classificationTable = table.computeClassificationTable();
                    break;
                case OUTPUT_SWITCH:
                    String outputFolderPath = args[++returnObject.consumedArguments];
                    //Create folder for results if does not exist
                    File folderFile = new File(outputFolderPath);
                    if (!folderFile.exists()) {
                        if (!folderFile.mkdirs()) {
                            throw new IllegalArgumentException("Cannot create folder.");
                        }
                    }
                    returnObject.outputFolderPath = outputFolderPath;
                    break;
                case TEMP_SWITCH:
                    returnObject.tempFolderPath = args[++returnObject.consumedArguments];
                    break;
                case INPUTS_SWITCH:
                    inputsBeingEnumerated = true;
                    break;
                case PRINT_PROGRESS_SWITCH:
                    returnObject.makeOutputs = true;
                    break;
                case SUPPRESS_PROGRESS_SWITCH:
                    returnObject.makeOutputs = false;
                    break;
                case ONLY_PRIOR_SWITCH_SWITCH:
                    returnObject.onlyPriorProbability = true;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid option for classification: " + nextArgument);
            }
        }

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
        copy.classificationTable = classificationTable == null ? null : classificationTable.makeCopy();
        copy.priorProbability = priorProbability == null ? null : priorProbability.makeCopy();
        copy.inputPaths = inputPaths;
        return copy;
    }

    @Override
    public String toString() {
        return "ClassificationConfiguration{" +
                "consumedArguments=" + consumedArguments +
                ", batchType=" + batchType +
                ", priorType=" + priorType +
                ", exportType=" + exportType +
                ", memoryType=" + memoryType +
                ", classificationTable=" + classificationTable +
                ", outputFolderPath='" + outputFolderPath + '\'' +
                ", tempFolderPath='" + tempFolderPath + '\'' +
                ", makeOutputs=" + makeOutputs +
                ", onlyPriorProbability=" + onlyPriorProbability +
                ", inputPaths=" + inputPaths +
                ", keyCount=" + keyCount +
                ", rngSeed=" + rngSeed +
                ", priorProbability=" + priorProbability +
                '}';
    }
}
