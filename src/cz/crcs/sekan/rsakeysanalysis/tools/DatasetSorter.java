package cz.crcs.sekan.rsakeysanalysis.tools;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.ClassificationConfiguration;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import cz.crcs.sekan.rsakeysanalysis.common.FileIterator;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Partially sorts dataset based on moduli.
 * When removing duplicities, keys must be held in memory until all their versions are processed.
 * It helps to have keys close together, in order to avoid having many open batches at the same time.
 * This presorts the dataset to get keys closer together.
 * TODO Since we are only interested in duplicate moduli (not similar), hashing makes more sense than raw values,
 * since values will be distributed more uniformly.
 *
 * @author xnemec1
 * @version 3/7/17.
 */
public class DatasetSorter {

    public static void preSortDataset(int prefixBits, String outputFolderName, String tempFolderName,
                                      List<String> datasetFilePaths)
            throws DataSetException, NoSuchAlgorithmException, IOException {
        // number of prefix bits for presorting -- this will produce 2^prefixBits files
        if (prefixBits <= 1) throw new IllegalArgumentException("Configure the number of prefix bits!");

        File outDir = new File(outputFolderName);
        File tempDir = new File(tempFolderName);
        if (!tempDir.exists() && !tempDir.mkdir()) {
            System.err.println("Could not create temporary directory " + tempDir.getName());
            return;
        }

        for (String datasetFilePath : datasetFilePaths) {

            FileIterator fileIterator = new FileIterator(datasetFilePath);

            Random random = new Random();
            File randomTempDir = new File(tempDir, "temp_" + random.nextLong());
            if (randomTempDir.exists() || !randomTempDir.mkdir()) {
                System.err.println("Could not create random temp directory " + randomTempDir.getName());
                return;
            }

            List<Integer> fileIDs = new ArrayList<>(1 << (prefixBits - 1));

            Map<Integer, ExtendedWriter> prefixToWriter = new TreeMap<>();
            for (Integer i = 1 << (prefixBits - 1); i < 1 << prefixBits; i++) {
                fileIDs.add(i);
                prefixToWriter.put(i, new ExtendedWriter(new File(randomTempDir, i.toString())));
            }

            long processed = 0L;
            while (fileIterator.hasNext()) {
                String line = fileIterator.next();
                BigInteger modulus = ClassificationKey.getModulusFromJSON(line);
                if (modulus == null) {
                    System.err.println("Could not extract modulus from line:");
                    System.err.println(line);
                    continue;
                }
                int length = modulus.bitLength();
                BigInteger prefixModulus = modulus.shiftRight(length - prefixBits);
                ExtendedWriter writer = prefixToWriter.get(prefixModulus.intValueExact());
                writer.writeln(line);
                if (++processed % 100000 == 0) System.out.println(String.format("Processed %d lines", processed));
            }

            fileIterator.close();
            for (Integer i : fileIDs) {
                prefixToWriter.get(i).close();
            }

            File inputFile = new File(datasetFilePath);
            if (outDir.equals(inputFile.getParentFile())) {
                inputFile = new File(inputFile.getParentFile(), "presorted_" + inputFile.getName());
            }

            try (ExtendedWriter fullWriter = new ExtendedWriter(new File(outDir, inputFile.getName()))) {
                for (Integer i : fileIDs) {
                    processed = 0L;
                    File tempFile = new File(randomTempDir, i.toString());
                    FileIterator iterator = new FileIterator(tempFile);
                    while (iterator.hasNext()) {
                        fullWriter.writeln(iterator.next());
                        processed++;
                    }
                    iterator.close();
                    System.out.println(String.format("File %d contained %d lines", i, processed));
                    if (!tempFile.delete()) {
                        System.err.println("Could not delete the temporary file " + tempFile.getName());
                    }
                }
                fullWriter.close();
                if (!randomTempDir.delete()) {
                    System.err.println("Could not delete the temporary directory " + randomTempDir.getName());
                }
            } catch (IOException e) {
                System.err.println("Could not write the presorted dataset");
            }
        }
    }

}
