package cz.crcs.sekan.rsakeysanalysis.tools;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset.DataSetFormatter;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset.FileDataSetIterator;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import cz.crcs.sekan.rsakeysanalysis.common.FileIterator;
import cz.crcs.sekan.rsakeysanalysis.common.JSONObjectMerger;
import cz.crcs.sekan.rsakeysanalysis.common.JSONPropertyExtractor;
import cz.crcs.sekan.rsakeysanalysis.common.exception.WrongKeyException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * @author xnemec1
 * @version 3/1/17.
 */
public class DuplicityRemover {

    private static class DuplicityCounter {
        private Map<BigInteger, Integer> duplicityCounter;

        public DuplicityCounter() {
            this.duplicityCounter = new HashMap<>();
        }

        public Integer add(BigInteger modulus) {
            return duplicityCounter.compute(modulus, (key, oldValue) -> oldValue == null ? 1 : oldValue + 1);
        }

        public Integer decreaseDuplicityCount(BigInteger modulus) {
            Integer newCount = duplicityCounter.computeIfPresent(modulus, (key, count) -> count - 1);
            if (newCount == null) return -1;
            if (newCount <= 0) duplicityCounter.remove(modulus);
            return newCount;
        }

        public Integer keyCount() {
            return duplicityCounter.size();
        }
    }

    public static void checkPaths(String outputDirPath, String inputPath) {
        File outputDir = new File(outputDirPath);
        if (outputDir.exists()) {
            if (!outputDir.isDirectory()) {
                System.err.println("Output path exists, but is not a directory");
                throw new IllegalArgumentException("Output path exists, but is not a directory");
            }
            if (outputDir.getAbsolutePath().equals(new File(inputPath).getParentFile().getAbsolutePath())) {
                System.err.println("Output path points to input pahts, this would overwrite the original files");
                throw new IllegalArgumentException("Output path points to input pahts, this would overwrite the original files");
            }
        } else {
            if (!outputDir.mkdir()) {
                System.err.println("Cannot create output directory");
                throw new IllegalArgumentException("Cannot create output directory");
            }
        }
    }

    public static void removeDuplicitiesBatch(String outputDirPath, DataSetFormatter formatter, String... filePaths) throws DataSetException {
        checkPaths(outputDirPath, filePaths[0]);

        for (String filePath : filePaths) {
            System.out.println(String.format("Removing duplicities from file: %s", filePath));
            removeDuplicities(filePath, new File(outputDirPath, new File(filePath).getName()).getAbsolutePath(),
                    formatter, JSONPropertyExtractor.MODULUS_CASE_INSENSITIVE_EXTRACTOR);
        }
    }

    /**
     *
     * @param filePath
     * @param outputFilePath
     * @param formatter TODO: ignored, all output is JSON
     * @param jsonPropertyExtractor
     * @throws DataSetException
     */
    public static void removeDuplicities(String filePath, String outputFilePath, DataSetFormatter formatter,
                                         JSONPropertyExtractor jsonPropertyExtractor) throws DataSetException {
        Integer allKeys = 0;

        // first pass -- find number of duplicities
        FileIterator file = new FileIterator(filePath);
        DuplicityCounter counter = new DuplicityCounter();
        while (file.hasNext()) {
            String line = file.next();
            BigInteger propertyHash = jsonPropertyExtractor.extractHashOfProperty(line);
            if (propertyHash == null) {
                System.err.println("Could not extract property hash from line:");
                System.err.println(line);
                continue;
            }
            counter.add(propertyHash);
            allKeys++;
        }

        System.out.println(String.format("Dataset contains %d items, with %d unique items", allKeys, counter.keyCount()));

        // second pass -- directly output unique keys; only hold keys to be merged until all have been seen
        Map<BigInteger, List<JSONObject>> mergedKeys = new HashMap<>();

        FileIterator jsonIterator = new FileIterator(filePath);

        try (ExtendedWriter datasetWriter = new ExtendedWriter(new File(outputFilePath))) {
            long time = System.currentTimeMillis();
            Long read = 0L;
            Long printFrequency = 100000L;
            Long printed = 0L;
            JSONParser parser = new JSONParser();
            while (jsonIterator.hasNext()) {
                JSONObject readKey;
                String jsonLine;
                try {
                    jsonLine = jsonIterator.next();
                    readKey = (JSONObject) parser.parse(jsonLine);
                } catch (ParseException e) {
                    System.err.println("Failed to parse JSON line " + e.toString());
                    continue;
                }
                BigInteger propertyHash = jsonPropertyExtractor.extractHashOfProperty(jsonLine);
                if (propertyHash == null) {
                    System.err.println("Could not extract property hash from line:");
                    System.err.println(jsonLine);
                    continue;
                }
                Integer leftInDataset = counter.decreaseDuplicityCount(propertyHash);

                List<JSONObject> resultingKeys = mergedKeys.get(propertyHash);
                if (resultingKeys == null) {
                    resultingKeys = new ArrayList<>(1);
                }
                resultingKeys.add(readKey);

                if (leftInDataset <= 0) {
                    JSONObject mergedKey = JSONObjectMerger.mergeJSONObjects(resultingKeys);
                    if (mergedKey == null) {
                        System.err.println("ERROR Could not merge objects");
                    } else {
                        datasetWriter.writeln(mergedKey.toJSONString());
                        mergedKeys.remove(propertyHash);
                        ++printed;
                    }
                } else {
                    mergedKeys.put(propertyHash, resultingKeys);
                }

                if (++read % printFrequency == 0) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - time;
                    System.out.println(String.format(
                            "Parsed %d keys (%d output, %d in memory) in %d seconds (%d per second) %d MB memory usage",
                            read, printed, mergedKeys.size(), elapsedTime / 1000,
                            printFrequency * 1000 / elapsedTime, Runtime.getRuntime().totalMemory() / 1000000));
                    time = currentTime;
                }
            }
        } catch (IOException e) {
            System.err.println("Error when writing unique dataset");
            e.printStackTrace();
        }
    }

    public static void printFirstModulus(String outputDirPath, DataSetFormatter formatter, List<String> filePaths)
            throws DataSetException {
        checkPaths(outputDirPath, filePaths.get(0));

        printFirstOccurrence(filePaths, outputDirPath,
                             formatter, JSONPropertyExtractor.MODULUS_CASE_INSENSITIVE_EXTRACTOR);
    }

    public static void printFirstHash(String outputDirPath, DataSetFormatter formatter, List<String> filePaths)
            throws DataSetException {
        checkPaths(outputDirPath, filePaths.get(0));

        printFirstOccurrence(filePaths, outputDirPath,
                formatter, JSONPropertyExtractor.FINGERPRINT_CASE_INSENSITIVE_EXTRACTOR);
    }

    private static void printFirstOccurrence(List<String> filePaths, String outputDirPath, DataSetFormatter formatter,
                                             JSONPropertyExtractor jsonPropertyExtractor) throws DataSetException {
        Integer allKeys = 0;
        Integer printedKeys = 0;
        DuplicityCounter counter = new DuplicityCounter();

        for (String filePath : filePaths) {
            File outputFile = new File(outputDirPath, new File(filePath).getName());
            try (ExtendedWriter datasetWriter = new ExtendedWriter(outputFile)) {
                FileIterator file = new FileIterator(filePath);
                while (file.hasNext()) {
                    String line = file.next();
                    BigInteger propertyHash = jsonPropertyExtractor.extractHashOfProperty(line);
                    if (propertyHash == null) {
                        System.err.println("Could not extract property hash from line:");
                        System.err.println(line);
                        continue;
                    }
                    Integer newCount = counter.add(propertyHash);
                    if (newCount == 1) {
                        datasetWriter.writeln(line);
                        printedKeys++;
                    }
                    allKeys++;
                }
            } catch (IOException e) {
                System.err.println("Error when writing unique dataset");
                e.printStackTrace();
            }
        }

        System.out.println(String.format("Parsed %d, wrote %d", allKeys, printedKeys));
    }

}
