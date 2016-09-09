package cz.crcs.sekan.rsakeysanalysis.template;

import cz.crcs.sekan.rsakeysanalysis.Main;

import java.io.*;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 20.04.2016
 */
public class Template {
    /**
     * Content of template
     */
    private String content;

    /**
     * Create template object from file with specified name in folder templates
     * @param name name of file in folder templates
     * @throws IOException
     */
    public Template(String name) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResourceAsStream("templates/" + name)));
        StringBuilder output = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            output.append(line);
            output.append('\n');
        }
        content = output.toString();
    }

    /**
     * Set content of all occurrences of variable {$name} in template
     * @param name name of variable
     * @param value content of variable
     */
    public void setVariable(String name, String value) {
        content = content.replace("{$" + name + "}", value);
    }

    /**
     * Save generated template to file
     * @param outfile path to file
     * @throws IOException
     */
    public void generateFile(String outfile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfile))) {
            writer.write(content);
        }
    }
}
