package cz.crcs.sekan.rsakeysanalysis.tools;

import cz.crcs.sekan.rsakeysanalysis.classification.table.RawTable;
import cz.crcs.sekan.rsakeysanalysis.template.Template;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Map;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 12.06.2016
 */
public class EuclideanDistanceOfSources {
    public static void run(String infile, String outfile) throws Exception {
        RawTable table = RawTable.load(infile);
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
        otherSymbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("0.000", otherSymbols);

        Template template = new Template("edos.html");

        Map<String, Map<String, Double>> ed = table.computeEuclideanDistances();
        //Header
        String tableToTemplate = "<table class=\"table table-bordered table-condensed\">";
        tableToTemplate += "<thead><tr><th> </th><th>" + String.join("</th><th>", ed.keySet()) + "</th></tr></thead><tbody>";

        //Body
        for (String source : ed.keySet()) {
            tableToTemplate += "<tr><th>" + source + "</th>";
            for (String otherSource : ed.keySet()) {
                double value = ed.get(source).get(otherSource);
                String color = "#f2dede";
                if (value <= table.getMaxEuclideanDistanceForGroup()*1.5) color = "#fcf8e3";
                if (value <= table.getMaxEuclideanDistanceForGroup()) color = "#d9edf7";
                if (value <= table.getMaxEuclideanDistanceForGroup()*0.5) color = "#dff0d8";
                tableToTemplate += "<td style=\"background-color: " + color + ";\">" + df.format(value) + "</td>";
            }
            tableToTemplate += "</tr>";
        }

        //Footer
        tableToTemplate += "</tbody></table>";

        template.setVariable("table", tableToTemplate);
        template.generateFile(outfile);
    }
}
