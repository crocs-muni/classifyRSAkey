package cz.crcs.sekan.rsakeysanalysis.tools;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset.DataSetFormatter;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset.FileDataSetIterator;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import cz.crcs.sekan.rsakeysanalysis.common.FileIterator;
import cz.crcs.sekan.rsakeysanalysis.common.exception.WrongKeyException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

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

    public static void removeDuplicities(String filePath, String outputFilePath, DataSetFormatter formatter) throws DataSetException {
        Integer allKeys = 0;

        // first pass -- find number of duplicities
        FileIterator file = new FileIterator(filePath);
        DuplicityCounter counter = new DuplicityCounter();
        while (file.hasNext()) {
            String line = file.next();
            BigInteger modulus = ClassificationKey.getShortenedModulusFromJSON(line);
            if (modulus == null) {
                System.err.println("Could not extract modulus from line:");
                System.err.println(line);
                continue;
            }
            counter.add(modulus);
            allKeys++;
        }

        System.out.println(String.format("Dataset contains %d keys, with %d unique keys", allKeys, counter.keyCount()));

        // second pass -- directly output unique keys; only hold keys to be merged until all have been seen
        Map<BigInteger, ClassificationKey> mergedKeys = new HashMap<>();

        FileDataSetIterator dataSetIterator = new FileDataSetIterator(filePath);

        try (ExtendedWriter datasetWriter = new ExtendedWriter(new File(outputFilePath))) {
            long time = System.currentTimeMillis();
            Long read = 0L;
            Long printFrequency = 100000L;
            Long printed = 0L;
            while (dataSetIterator.hasNext()) {
                ClassificationKey readKey = dataSetIterator.next();
                BigInteger shortModulus = readKey.getShortenedModulus();
                if (shortModulus == null) {
                    System.err.println("Could not extract modulus from key:");
                    System.err.println(readKey.toJSON());
                    continue;
                }
                Integer leftInDataset = counter.decreaseDuplicityCount(shortModulus);

                ClassificationKey resultingKey = mergedKeys.compute(shortModulus, (shortenedModulus, mergedKey) -> {
                    try {
                        return mergedKey == null ? readKey : mergedKey.mergeWith(readKey);
                    } catch (WrongKeyException e) {
                        System.err.println("Could not merge two supposedly same keys: " + shortModulus.toString(16)
                                + " new: " + readKey.getRsaKey().getModulus().toString(16)
                                + " old: " + mergedKey.getRsaKey().getModulus().toString(16));
                        return mergedKey;
                    }
                });

                if (leftInDataset <= 0) {
                    datasetWriter.writeln(formatter.originalKeyToLine(resultingKey));
                    mergedKeys.remove(shortModulus);
                    ++printed;
                }

                if (++read % printFrequency == 0) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - time;
                    System.out.println(String.format("Parsed %d keys (%d output, %d in memory) in %d seconds (%d per second) %d MB memory usage",
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

}
