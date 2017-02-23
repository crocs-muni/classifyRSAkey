package cz.crcs.sekan.rsakeysanalysis.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 26.04.2016
 */
public class ExtendedWriter extends BufferedWriter {
    public ExtendedWriter(File file) throws IOException {
        super(new FileWriter(file));
    }

    public ExtendedWriter(String file) throws IOException {
        super(new FileWriter(file));
    }

    public void writeln(String line) throws IOException {
        write(line);
        newLine();
    }
}
