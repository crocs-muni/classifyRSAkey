package cz.crcs.sekan.rsakeysanalysis;

import cz.crcs.sekan.rsakeysanalysis.classification.DataSetClassification;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.makefile.Makefile;
import cz.crcs.sekan.rsakeysanalysis.classification.table.RawTable;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.ClassificationSuccess;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.Misclassification;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.ModulusFactors;
import cz.crcs.sekan.rsakeysanalysis.tools.*;

import java.util.*;

public class Main {

    /**
     * Static class for identification which factors check on modulus (null = do not check factors).
     */
    private static ModulusFactors modulusFactors = null;

    /**
     * Classification table for identification which keys should not be classified.
     */
    private static ClassificationTable classificationTableForNotClassify = null;

    /**
     * Main function. For details see showHelp function.
     * @param args arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-m":
                case "--make":
                    buildTable(args[++i], args[++i]);
                    break;
                case "-i":
                case "--info":
                    tableInformation(args[++i]);
                    break;
                case "-ed":
                case "--sourcesED":
                    EuclideanDistanceOfSources.run(args[++i], args[++i]);
                    break;
                case "-ec":
                case "--exportClassificationCsv":
                    exportClassificationTableToCsv(args[++i], args[++i]);
                    break;
                case "-er":
                case "--exportRawCsv":
                    exportRawTableToCsv(args[++i], args[++i]);
                    break;
                case "-cs":
                case "--classificationSuccess":
                    ClassificationSuccess.compute(args[++i], args[++i], Long.valueOf(args[++i]));
                    break;
                case "-mc":
                case "--misclassification":
                    String mInFile = args[++i], mOutFile = args[++i];
                    long mKeys = Long.valueOf(args[++i]);
                    Misclassification misclassification = new Misclassification(mInFile, mKeys);
                    misclassification.compute(mOutFile);
                    break;
                case "-pgp":
                case "--convertPgp":
                    PgpSetToDataSet.run(args[++i], args[++i]);
                    break;
                case "-f":
                case "--factors":
                    modulusFactors = new ModulusFactors(args[++i]);
                    break;
                case "-c":
                case "--classify":
                    RawTable table = RawTable.load(args[++i]);
                    ClassificationTable classificationTable = table.computeClassificationTable();
                    classificationTable.setRawTable(table);
                    classifyDataSet(classificationTable, args[++i], args[++i]);
                    break;
                case "-cf":
                case "--classifyFactorable":
                    RawTable tableFactorable = RawTable.load(args[++i]);
                    ClassificationTable classificationTableFactorable = tableFactorable.computeClassificationTable();
                    classificationTableFactorable.setRawTable(tableFactorable);
                    classifyDataSet(classificationTableFactorable, args[++i], args[++i], true);
                    break;
                case "-d":
                case "--diff":
                    DatasetsDiff.run(args[++i], args[++i], args[++i]);
                    break;
                case "-rd":
                case "--removeDuplicity":
                    RemoveDuplicity.run(args[++i], args[++i]);
                    break;
                case "-nc":
                case "--nc":
                    classificationTableForNotClassify = RawTable.load(args[++i]).computeClassificationTable();
                    break;
                case "-h":
                case "--help":
                    showHelp();
                    break;
                case "-cr":
                case "--cumulateResults":
                    CumulateResults.run(args[++i], args[++i]);
                    break;
                case "-bh":
                case "--batchesHistogram":
                    BatchesHistogram.run(args[++i], args[++i]);
                    break;
                case "-csv":
                case "--convertToCsv":
                    RawTable tableForCsv = RawTable.load(args[++i]);
                    ClassificationTable classificationTableForCsv = tableForCsv.computeClassificationTable();
                    classificationTableForCsv.setRawTable(tableForCsv);
                    JsonSetToCsv.run(classificationTableForCsv, args[++i], args[++i], args[++i], args[++i]);
                    break;
                default:
                    System.out.println("Undefined parameter '" + args[i] + "'");
            }
        }

        if (args.length == 0) {
            showHelp();
        }
    }

    /**
     * Helper function for show information about all application flags.
     */
    private static void showHelp() {
        System.out.println("RSAKeyAnalysis tool, CRoCS 2016");
        System.out.println("Options:\n" +
                "  -m   make  out       Build classification table from makefile.\n" +
                "                        make  = path to makefile\n" +
                "                        out   = path to json file (classification table file)\n" +
                "  -i   table           Load classification table and show information about it.\n" +
                "                        table = path to classification table file\n" +
                "  -ed  table out       Create table showing euclidean distance of sources.\n" +
                "                        table = path to classification table file\n" +
                "                        out   = path to html file\n" +
                "  -ec  table out       Convert classification table to out format.\n" +
                "                        table = path to classification table file\n" +
                "                        out   = path to csv file\n" +
                "  -er  table out       Export raw table (usable for generate dendrogram)\n" +
                "                        table = path to classification table file\n" +
                "                        out   = path to csv file\n" +
                "  -cs  table out keys  Compute classification success.\n" +
                "                        table = path to classification table file\n" +
                "                        out   = path to csv file\n" +
                "                        keys  = how many keys will be used for test.\n" +
                "  -mc  table out keys  Compute misclassification rate.\n" +
                "                        table = path to classification table file\n" +
                "                        out   = path to csv file\n" +
                "                        keys  = how many keys will be used for test.\n" +
                "  -pgp in    out       Convert pgp keys to format for classification.\n" +
                "                        in    = path to json file (dump of pgp key set)\n" +
                "                                http://pgp.key-server.io/dump/\n" +
                "                                https://github.com/diafygi/openpgp-python\n" +
                "                        out   = path to json file (classification key set)\n" +
                "  -f   in              Load factors which have to be try on modulus of key.\n" +
                "                        in    = path to txt file\n" +
                "                                Each line of file contains one factor (hex).\n" +
                "  -c   table in  out   Classify keys from key set.\n" +
                "                        table = path to classification table file\n" +
                "                        in    = path to key set\n" +
                "                        out   = path to folder for storing results\n" +
                "  -cf  table in  out   Classify private keys which share some factors (factored by batch GCD) from key set.\n" +
                "                        table = path to classification table file\n" +
                "                        in    = path to key set\n" +
                "                        out   = path to folder for storing results\n" +
                "  -rd  in    out       Remove duplicity from key set.\n" +
                "                        in    = path to key set\n" +
                "                        out   = path to key set\n" +
                "  -nc  table           Set classification table for not classify some keys.\n" +
                "                        table = path to classification table file\n" +
                "  -h                   Show this help.\n" +
                "" +
                "");
        //TODO complete all options
    }

    /**
     * Build classification table by makefile.
     * @param makefile path to makefile
     * @param outfile path to output file (classification table)
     * @throws Exception
     */
    private static void buildTable(String makefile, String outfile) throws Exception {
        Makefile make = new Makefile(makefile);
        RawTable table = make.make();
        table.save(outfile);
    }

    /**
     * Show information about classification table
     * @param infile path to classification table
     * @throws Exception
     */
    private static void tableInformation(String infile) throws Exception {
        RawTable table = RawTable.load(infile);

        Map<String, Long> counts = table.computeSourcesCount();
        System.out.println("Number of sources: " + counts.keySet().size());
        System.out.println("Number of keys: " + counts.values().stream().mapToLong(Long::longValue).sum());

        Set<Set<String>> groups = table.computeSourceGroups();
        System.out.println("Groups (" + groups.size() + "): ");
        for (Set<String> group : groups) {
            System.out.println("\t" + String.join(", ", group));
        }
    }

    /**
     * Convert classification table to csv format
     * @param infile path to json classification table
     * @param outfile path to new csv classification table
     * @throws Exception
     */
    private static void exportClassificationTableToCsv(String infile, String outfile) throws Exception {
        RawTable table = RawTable.load(infile);
        ClassificationTable classificationTable = table.computeClassificationTable();
        classificationTable.exportToCsvFormat(outfile);
    }

    /**
     * Convert raw table to csv format
     * @param infile path to json classification table
     * @param outfile path to csv raw table file
     * @throws Exception
     */
    private static void exportRawTableToCsv(String infile, String outfile) throws Exception {
        RawTable table = RawTable.load(infile);
        table.exportToCsvFormat(outfile);
    }

    /**
     * Classify key set.
     * @param table classification table
     * @param fileName path to key set file
     * @param folder path to folder where will be create files with results
     * @param batchBySharedPrimes batch the private keys by shared primes (true) or with default batching (false)
     */
    private static void classifyDataSet(ClassificationTable table, String fileName, String folder, boolean batchBySharedPrimes) {
        DataSetClassification classification = new DataSetClassification(table, fileName, folder);
        if (modulusFactors != null) classification.setModulusFactors(modulusFactors);
        if (classificationTableForNotClassify != null) classification.setClassificationTableForNotClassify(classificationTableForNotClassify);
        classification.classify(batchBySharedPrimes);
    }

    /**
     * Classify key set.
     * @param table classification table
     * @param fileName path to key set file
     * @param folder path to folder where will be create files with results
     */
    private static void classifyDataSet(ClassificationTable table, String fileName, String folder) {
        classifyDataSet(table, fileName, folder, false);
    }
}
