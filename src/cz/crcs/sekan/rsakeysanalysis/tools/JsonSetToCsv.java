package cz.crcs.sekan.rsakeysanalysis.tools;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.template.Template;

import java.io.*;
import java.util.Set;

/**
 * Created by xnemec1 on 11/7/16.
 */
public class JsonSetToCsv {
    public static void run(ClassificationTable classificationTable, String infile, String outfile, String templateHeader, String templateName) {
        int line = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(infile))) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfile))) {
                String jsonLine;

                templateHeader = "vector," + templateHeader;

                Template template = new Template(templateName);
                if (templateName != null && !templateName.isEmpty()) {
                    writer.write(templateHeader);
                    writer.newLine();
                }

                while ((jsonLine = reader.readLine()) != null) {
                    ClassificationKey key = ClassificationKey.fromJson(jsonLine);

                    String vector = classificationTable.generationIdentification(key);
                    writer.write(vector + "," + key.toStringByTemplate(template));

                    line++;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("Error: cannot parse line " + line + ": " + ex.getMessage());
            }
        }
        catch (IOException ex) {
            System.err.println("Error while reading file '" + infile + "'.");
        }
    }

}
