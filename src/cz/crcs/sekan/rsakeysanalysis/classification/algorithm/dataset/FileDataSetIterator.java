package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.common.FileIterator;
import cz.crcs.sekan.rsakeysanalysis.common.exception.WrongKeyException;
import org.json.simple.parser.ParseException;

import java.io.*;

/**
 * @author xnemec1
 * @version 11/23/16.
 */
public class FileDataSetIterator implements DataSetIterator {

    private FileIterator fileIterator;

    private String datasetName;

    public FileDataSetIterator(String pathToDataset) throws DataSetException {
        this.fileIterator = new FileIterator(pathToDataset);
        this.fileIterator.rewind();
        this.datasetName = new File(pathToDataset).getName();
    }

    @Override
    public boolean hasNext() {
         return fileIterator.hasNext();
    }

    @Override
    public ClassificationKey next() {
        String line = fileIterator.next();

        if (line == null) return null;
        if (line.length() > 16*1024*1024) {
            System.err.println(String.format("WARNING: skipping a long line (%d characters): %s ...", line.length(), line.substring(0, 128)));
            return next();
        }

        ClassificationKey nextKey;

        try {
            nextKey = ClassificationKey.fromJson(line);
        } catch (ParseException | WrongKeyException e) {
            System.err.println("Warning: cannot parse dataset line: " + e.getMessage());
            System.err.println(line);
            nextKey = null;
        }
        return nextKey;
    }

    @Override
    public void close() {
        fileIterator.close();
    }

    @Override
    public String getDataSetName() {
        return datasetName;
    }
}
