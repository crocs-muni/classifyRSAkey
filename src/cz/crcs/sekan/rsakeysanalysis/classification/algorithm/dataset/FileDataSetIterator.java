package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.common.exception.WrongKeyException;
import org.json.simple.parser.ParseException;

import java.io.*;

/**
 * @author xnemec1
 * @version 11/23/16.
 */
public class FileDataSetIterator implements DataSetIterator {

    private BufferedReader reader;

    private ClassificationKey nextKey;

    private String pathToDataset;

    public FileDataSetIterator(String pathToDataset) throws DataSetException {
        this.pathToDataset = pathToDataset;
        rewind();
    }

    @Override
    public boolean hasNext() {
         return nextKey != null;
    }

    @Override
    public ClassificationKey next() {
        if (reader == null) throw new IllegalStateException("Cannot iterate over closed file");
        ClassificationKey lastKey = nextKey;
        try {
            prepareNextKey();
        } catch (IOException e) {
            System.err.println("Error while reading dataset file: " + e.getMessage());
        }
        return lastKey;
    }

    @Override
    public void close() {
        if (reader != null) try {
            reader.close();
            reader = null;
        } catch (IOException e) {
            System.err.printf("Could not close dataset reader " + e.getMessage());
        }
    }

    private void rewind() throws DataSetException {
        if (reader != null) {
            close();
        }

        try {
            reader = new BufferedReader(new FileReader(new File(pathToDataset)));
            prepareNextKey();
        } catch (IOException e) {
            throw new DataSetException(e);
        }
    }

    private boolean prepareNextKey() throws IOException {
        nextKey = null;

        while (nextKey == null) {
            String jsonLine = reader.readLine();

            if (jsonLine == null) {
                nextKey = null;
                return false; // end of dataset
            }

            if (jsonLine.isEmpty()) continue;

            try {
                nextKey = ClassificationKey.fromJson(jsonLine);
            } catch (ParseException | WrongKeyException e) {
                System.err.println("Warning: cannot parse dataset line: " + e.getMessage());
                System.err.println(jsonLine);
                nextKey = null;
            }
        }

        return true;
    }
}
