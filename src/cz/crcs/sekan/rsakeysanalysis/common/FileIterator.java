package cz.crcs.sekan.rsakeysanalysis.common;

import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.common.exception.WrongKeyException;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author xnemec1
 * @version 3/1/17.
 */
public class FileIterator implements Iterator<String> {

    private BufferedReader reader;

    private String nextLine;

    private String pathToFile;

    public FileIterator(String pathToFile) throws DataSetException {
        this.pathToFile = pathToFile;
        rewind();
    }

    @Override
    public boolean hasNext() {
        return nextLine != null;
    }

    @Override
    public String next() {
        if (reader == null) throw new IllegalStateException("Cannot iterate over closed file");
        String lastLine = nextLine;
        try {
            prepareNextLine();
        } catch (IOException e) {
            System.err.println("Error while reading dataset file: " + e.getMessage());
        }
        return lastLine;
    }

    public void close() {
        if (reader != null) try {
            reader.close();
            reader = null;
        } catch (IOException e) {
            System.err.printf("Could not close dataset reader " + e.getMessage());
        }
    }

    public void rewind() throws DataSetException {
        if (reader != null) {
            close();
        }

        try {
            reader = new BufferedReader(new FileReader(new File(pathToFile)));
            prepareNextLine();
        } catch (IOException e) {
            throw new DataSetException(e);
        }
    }

    private boolean prepareNextLine() throws IOException {
        nextLine = null;

        while (nextLine == null) {
            String line = reader.readLine();

            if (line == null) {
                nextLine = null;
                return false; // end of dataset
            }

            if (line.isEmpty()) continue;

            nextLine = line;
        }

        return true;
    }
}
