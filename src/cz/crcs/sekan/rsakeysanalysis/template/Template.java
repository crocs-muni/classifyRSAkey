package cz.crcs.sekan.rsakeysanalysis.template;

import cz.crcs.sekan.rsakeysanalysis.Main;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

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
     * Variables
     */
    private Map<String, String> variables = new HashMap<>();

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
        variables.put(name, value);
    }

    /**
     * Clear all variables values
     */
    public void resetVariables() {
        variables.clear();
    }
    /**
     * Save generated template to file
     * @param outfile path to file
     * @throws IOException
     */
    public void generateFile(String outfile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfile))) {
            writer.write(generateString());
        }
    }

    /**
     * Generate template to string
     * @return content of template with set variables
     */
    public String generateString() {
        String generated = content;
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            generated = generated.replace("{$" + variable.getKey() + "}", variable.getValue());
        }
        return generated;
    }
}
