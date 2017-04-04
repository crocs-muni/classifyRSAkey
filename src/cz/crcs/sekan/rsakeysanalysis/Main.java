package cz.crcs.sekan.rsakeysanalysis;

import cz.crcs.sekan.rsakeysanalysis.classification.DataSetClassification;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.Classification;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.ClassificationConfiguration;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.*;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset.JsonDataSetFormatter;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.ClassificationException;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.DataSetException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.RawTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.makefile.Makefile;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.TransformationNotFoundException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongTransformationFormatException;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.*;
import cz.crcs.sekan.rsakeysanalysis.tools.*;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
     *
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
                    //ClassificationSuccess.compute(args[++i], args[++i], Long.valueOf(args[++i]));
                    i = classificationSuccess(Arrays.copyOfRange(args, ++i, args.length));
                    break;
                case "-mc":
                case "--misclassification":
                    String mInFile = args[++i], mOutFile = args[++i];
                    long mKeys = Long.valueOf(args[++i]);
                    Misclassification misclassification = new Misclassification(mInFile, mKeys); // TODO new algorithm
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
                    i = classifyDataSet(Arrays.copyOfRange(args, ++i, args.length));
                    break;
                case "-d":
                case "--diff":
                    DatasetsDiff.run(args[++i], args[++i], args[++i]);
                    break;
                case "-rd":
                case "--removeDuplicity":
                    ClassificationConfiguration configuration = ClassificationConfiguration.fromCommandLineOptions(args, 1);
                    DuplicityRemover.removeDuplicitiesBatch(configuration.outputFolderPath, new JsonDataSetFormatter(),
                            configuration.inputPaths.toArray(new String[configuration.inputPaths.size()]));
                    i = configuration.consumedArguments;
                    break;
                case "-ps":
                case "--presort":
                    ClassificationConfiguration sortConfig = ClassificationConfiguration.fromCommandLineOptions(args, 1);
                    int prefixBits = sortConfig.keyCount + 1;
                    DatasetSorter.preSortDataset(prefixBits, sortConfig.outputFolderPath, sortConfig.tempFolderPath,
                            sortConfig.inputPaths);
                    i = sortConfig.consumedArguments;
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
                    JsonSetToCsv.run(classificationTableForCsv, args[++i], args[++i], args[++i], args[++i]);
                    break;
                case "-a":
                case "--apriori":
                    // aprioriTest(args[++i], args[++i])
                    ClassificationConfiguration config = ClassificationConfiguration.fromCommandLineOptions(args, 1);
                    i = config.consumedArguments;
                    //AprioriTest.testEstimatePrecision(config);
                    //AprioriTest.testPriorInfluence(config);
                    AprioriTest.testPriorEstimation(config);
                    //SimulationsTest.testSimulators(config);
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
        System.out.println("RSAKeyAnalysis tool, CRoCS 2017");
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
                "  -c   OPTIONS         Classify keys from key set.\n" +
                "                        OPTIONS = "
                + ClassificationConfiguration.CLASSIFICATION_TABLE_SWITCH + " table "
                + ClassificationConfiguration.INPUTS_SWITCH + " in... "
                + ClassificationConfiguration.OUTPUT_SWITCH + " outdir "
                + ClassificationConfiguration.BATCH_TYPE_SWITCH + " batch "
                + ClassificationConfiguration.PRIOR_TYPE_SWITCH + " prior "
                + ClassificationConfiguration.EXPORT_TYPE_SWITCH + " export "
                + ClassificationConfiguration.MEMORY_TYPE_SWITCH + " temp \n" +
                "                         " + ClassificationConfiguration.CLASSIFICATION_TABLE_SWITCH +
                " table  = path to classification table file\n" +
                "                         " + ClassificationConfiguration.INPUTS_SWITCH +
                " in     = path(s) to data set(s)\n" +
                "                         " + ClassificationConfiguration.OUTPUT_SWITCH +
                " out    = path to folder for storing results\n" +
                "                         " + ClassificationConfiguration.BATCH_TYPE_SWITCH +
                " batch  = " + Classification.BatchType.SOURCE +
                "|" + Classification.BatchType.PRIMES +
                "|" + Classification.BatchType.NONE + " -- how to batch keys\n" +
                "                         " + ClassificationConfiguration.PRIOR_TYPE_SWITCH +
                " prior  = " + Classification.PriorType.ESTIMATE +
                "|" + Classification.PriorType.UNIFORM + "|" + Classification.PriorType.TABLE +
                " -- prior probability\n" +
                "                         " + ClassificationConfiguration.EXPORT_TYPE_SWITCH +
                " export = " + Classification.ExportType.NONE +
                "|" + Classification.ExportType.JSON + "|" + Classification.ExportType.CSV +
                " -- annotated dataset export format\n" +
                "                         " + ClassificationConfiguration.MEMORY_TYPE_SWITCH +
                " temp   = " + Classification.MemoryType.NONE +
                "|" + Classification.MemoryType.DISK + "|" + Classification.MemoryType.MEMORY +
                " -- temporary memory handling - only for export\n" +
                "  -ps OPTIONS          Partially sort the dataset to make duplicity removal easier.\n" +
                "                        OPTIONS = "
                + ClassificationConfiguration.INPUTS_SWITCH + " in... "
                + ClassificationConfiguration.OUTPUT_SWITCH + " outdir "
                + ClassificationConfiguration.TEMP_SWITCH + " temp "
                + ClassificationConfiguration.KEY_COUNT_SWITCH + " prefix_bits \n" +
                "                        " + ClassificationConfiguration.INPUTS_SWITCH +
                " in... = paths to datasets\n" +
                "                        " + ClassificationConfiguration.OUTPUT_SWITCH +
                " out   = path to output directory for presorted dataset\n" +
                "                        " + ClassificationConfiguration.TEMP_SWITCH +
                " temp  = directory for temporary files\n" +
                "                        " + ClassificationConfiguration.KEY_COUNT_SWITCH +
                " bits  = number of prefix bits (makes 2^b temporary files)\n" +
                "  -rd " + ClassificationConfiguration.INPUTS_SWITCH + " ... "
                + ClassificationConfiguration.OUTPUT_SWITCH + " out    Remove duplicity from key set.\n" +
                "                        " + ClassificationConfiguration.OUTPUT_SWITCH +
                " outdir = directory for unique datasets\n" +
                "                        " + ClassificationConfiguration.INPUTS_SWITCH +
                " in...  = paths to key sets (processed individually)\n" +
                "  -a   table OPTIONS   Test a priori probability estimation.\n" +
                "                        table = path to classification table file\n" +
                "                        " + ClassificationConfiguration.KEY_COUNT_SWITCH +
                " runs  = number of random estimations\n" +
                "                        " + ClassificationConfiguration.RNG_SEED_SWITCH +
                " seed  = optional seed for RNG\n" +
                "  -nc  table           Set classification table for not classify some keys.\n" +
                "                        table = path to classification table file\n" +
                "  -h                   Show this help.\n" +
                "" +
                "");
        //TODO complete all options
    }

    /**
     * Build classification table by makefile.
     *
     * @param makefile path to makefile
     * @param outfile  path to output file (classification table)
     * @throws Exception
     */
    private static void buildTable(String makefile, String outfile) throws Exception {
        Makefile make = new Makefile(makefile);
        RawTable table = make.make();
        table.save(outfile);
    }

    /**
     * Show information about classification table
     *
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
     *
     * @param infile  path to json classification table
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
     *
     * @param infile  path to json classification table
     * @param outfile path to csv raw table file
     * @throws Exception
     */
    private static void exportRawTableToCsv(String infile, String outfile) throws Exception {
        RawTable table = RawTable.load(infile);
        table.exportToCsvFormat(outfile);
    }

    /**
     * Classify key set.
     *
     * @param table               classification table
     * @param fileName            path to key set file
     * @param folder              path to folder where will be create files with results
     * @param batchBySharedPrimes batch the private keys by shared primes (true) or with default batching (false)
     */
    @Deprecated
    private static void classifyDataSet(ClassificationTable table, String fileName, String folder, boolean batchBySharedPrimes) {
        DataSetClassification classification = new DataSetClassification(table, fileName, folder);
        if (modulusFactors != null) classification.setModulusFactors(modulusFactors);
        if (classificationTableForNotClassify != null)
            classification.setClassificationTableForNotClassify(classificationTableForNotClassify);
        classification.classify(batchBySharedPrimes);
    }

    /**
     * Classify key set.
     *
     * @param table    classification table
     * @param fileName path to key set file
     * @param folder   path to folder where will be create files with results
     */
    @Deprecated
    private static void classifyDataSet(ClassificationTable table, String fileName, String folder) {
        classifyDataSet(table, fileName, folder, false);
    }

    private static int classifyDataSet(String[] args)
            throws ClassificationException, IOException, ParseException, WrongTransformationFormatException, TransformationNotFoundException {

        Map<String, PriorProbabilityEstimator> dataSetNameToEstimator = new TreeMap<>();

        ClassificationConfiguration configuration = ClassificationConfiguration.fromCommandLineOptions(args, 0);
        for (String datasetFilePath : configuration.inputPaths) {
            Classification.Builder builder = Classification.BuildHelper.prepareBuilder(configuration.deepCopy(), datasetFilePath);

            Classification classification =  builder.build();
            PriorProbabilityEstimator priorProbabilityEstimator = classification.classify();
            dataSetNameToEstimator.put(classification.getDataSetName(), priorProbabilityEstimator);
        }

        return configuration.consumedArguments;
    }

    private static int classificationSuccess(String[] args)
            throws DataSetException, WrongTransformationFormatException, TransformationNotFoundException, ParseException, IOException, NoSuchAlgorithmException {

        ClassificationConfiguration configuration = ClassificationConfiguration.fromCommandLineOptions(args, 0);
        //ClassificationSuccessTest.runFromConfiguration(configuration);
        ClassificationSuccessTest.groupSuccess(configuration);
        //ClassificationSuccessTest.theoreticalSuccess(configuration);

        return configuration.consumedArguments;
    }

    // TODO proper test
    private static void aprioriTest(String tableFilePath, String datasetFilePath) throws IOException, ParseException, WrongTransformationFormatException, TransformationNotFoundException {
        RawTable table = RawTable.load(tableFilePath);
        ClassificationTable classificationTable = table.computeClassificationTable();

        //PriorProbabilityEstimator priorProbabilityEstimator = new LeastSquaresFitPriorProbabilityEstimator(classificationTable);
        //PriorProbabilityEstimator priorProbabilityEstimator = new LinearRegressionPriorProbabilityEstimator(classificationTable);
        PriorProbabilityEstimator priorProbabilityEstimator = new NonNegativeLeastSquaresFitPriorProbabilityEstimator(classificationTable);


        BufferedReader reader = new BufferedReader(new FileReader(datasetFilePath));
        String line;

        //BufferedWriter writer = new BufferedWriter(new FileWriter("shortened_tls.csv"));

        while ((line = reader.readLine()) != null) {
            priorProbabilityEstimator.addMask(line);
            String[] splitted = line.split("\\|");
            //writer.write(splitted[0] + "|" + splitted[2].substring(0, 2) + "\n");
        }

        reader.close();

        PriorProbability priorProbability = priorProbabilityEstimator.computePriorProbability();

        for (String group : priorProbability.keySet()) {
            System.out.println(group + " " + priorProbability.getGroupProbability(group) + " " + classificationTable.getGroupSources(group));
        }
    }


}
