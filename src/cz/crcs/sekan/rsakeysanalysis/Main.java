package cz.crcs.sekan.rsakeysanalysis;

import cz.crcs.sekan.rsakeysanalysis.classification.DataSetClassification;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.Classification;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.apriori.*;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.dataset.*;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.exception.ClassificationException;
import cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics.BatchesStatisticsAggregator;
import cz.crcs.sekan.rsakeysanalysis.classification.key.property.PrimePropertyExtractor;
import cz.crcs.sekan.rsakeysanalysis.classification.key.property.SourcePropertyExtractor;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.RawTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.makefile.Makefile;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.TransformationNotFoundException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongTransformationFormatException;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.ClassificationSuccess;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.Misclassification;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.ModulusFactors;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import cz.crcs.sekan.rsakeysanalysis.tools.*;
import org.json.simple.parser.ParseException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

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
                    ClassificationSuccess.compute(args[++i], args[++i], Long.valueOf(args[++i])); // TODO new algorithm
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
                    JsonSetToCsv.run(classificationTableForCsv, args[++i], args[++i], args[++i], args[++i]);
                    break;
                case "-a":
                case "--apriori":
                    aprioriTest(args[++i], args[++i]); // TODO new algorithm
                    break;
                default:
                    System.out.println("Undefined parameter '" + args[i] + "'");
            }
        }

        if (args.length == 0) {
            showHelp();
        }
    }

    private static final String BATCH_TYPE_SWITCH = "-b";

    private enum BatchType {
        SOURCE("source"),
        PRIMES("primes"),
        NONE("none");

        private final String name;

        BatchType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String PRIOR_TYPE_SWITCH = "-p";

    private enum PriorType {
        ESTIMATE("estimate"),
        UNIFORM("uniform"),
        TABLE("table");

        private final String name;

        PriorType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String EXPORT_TYPE_SWITCH = "-e";

    private enum ExportType {
        NONE("none"),
        JSON("json"),
        CSV("csv");

        private final String name;

        ExportType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String MEMORY_TYPE_SWITCH = "-t";

    private enum MemoryType {
        NONE("none"),
        DISK("disk"),
        MEMORY("memory");

        private final String name;

        MemoryType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
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
                "                        OPTIONS = table in out " + BATCH_TYPE_SWITCH + " batch "
                + PRIOR_TYPE_SWITCH + " prior "
                + EXPORT_TYPE_SWITCH + " export "
                + MEMORY_TYPE_SWITCH + " temp \n" +
                "                         table  = path to classification table file\n" +
                "                         in     = path to key set\n" +
                "                         out    = path to folder for storing results\n" +
                "                         batch  = " + BatchType.SOURCE + "|" + BatchType.PRIMES + "|" + BatchType.NONE + " -- how to batch keys\n" +
                "                         prior  = " + PriorType.ESTIMATE + "|" + PriorType.UNIFORM + "|" + PriorType.TABLE + " -- prior probability\n" +
                "                         export = " + ExportType.NONE + "|" + ExportType.JSON + "|" + ExportType.CSV + " -- annotated dataset export format\n" +
                "                         temp   = " + MemoryType.NONE + "|" + MemoryType.DISK + "|" + MemoryType.MEMORY + " -- only for export\n" +
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

        String tableFilePath = args[0];
        String datasetFilePath = args[1];
        String outputFolderPath = args[2];

        BatchType batchType = BatchType.SOURCE;
        PriorType priorType = PriorType.ESTIMATE;
        ExportType exportType = ExportType.NONE;
        MemoryType memoryType = MemoryType.DISK;

        if ((args.length - 3) % 2 != 0) {
            throw new IllegalArgumentException("Bad number of arguments, some switch might be missing an option");
        }

        int consumedArguments = 3;

        for (; consumedArguments < args.length; consumedArguments++) {
            switch (args[consumedArguments]) {
                case BATCH_TYPE_SWITCH:
                    batchType = BatchType.valueOf(args[++consumedArguments].toUpperCase());
                    break;
                case PRIOR_TYPE_SWITCH:
                    priorType = PriorType.valueOf(args[++consumedArguments].toUpperCase());
                    break;
                case EXPORT_TYPE_SWITCH:
                    exportType = ExportType.valueOf(args[++consumedArguments].toUpperCase());
                    break;
                case MEMORY_TYPE_SWITCH:
                    memoryType = MemoryType.valueOf(args[++consumedArguments].toUpperCase());
                    break;
                default:
                    throw new IllegalArgumentException("Invalid option for classification: " + args[consumedArguments]);
            }
        }


        RawTable table = RawTable.load(tableFilePath);
        ClassificationTable classificationTable = table.computeClassificationTable();

        //Create folder for results if does not exist
        File folderFile = new File(outputFolderPath);
        if (!folderFile.exists()) {
            if (!folderFile.mkdirs()) {
                throw new IllegalArgumentException("Cannot create folder.");
            }
        }

        Classification.Builder builder;

        switch (batchType) {
            case SOURCE:
                builder = new Classification.Builder<Set<String>>();
                builder.setPropertyExtractor(new SourcePropertyExtractor());
                break;
            case PRIMES:
                builder = new Classification.Builder<BigInteger>();
                builder.setPropertyExtractor(new PrimePropertyExtractor());
                break;
            case NONE:
            default:
                throw new NotImplementedException();
        }

        PriorProbabilityEstimator estimator;

        switch (priorType) {
            case ESTIMATE:
                estimator = new NonNegativeLeastSquaresFitPriorProbabilityEstimator(classificationTable);
                break;
            case UNIFORM:
                estimator = new UniformPriorProbabilityEstimator(classificationTable);
                break;
            case TABLE:
                estimator = new UserDefinedPriorProbabilityEstimator(classificationTable);
                break;
            default:
                throw new NotImplementedException();
        }

        DataSetFormatter formatter = null;
        DataSetSaver dataSetSaver;

        switch (exportType) {
            case CSV:
                formatter = new CsvDataSetFormatter();
                break;
            case JSON:
                formatter = new JsonDataSetFormatter();
                break;
            case NONE:
                formatter = null;
                dataSetSaver = new NoActionDataSetSaver();
                break;
            default:
                throw new NotImplementedException();
        }

        if (formatter == null) {
            dataSetSaver = new NoActionDataSetSaver();
        } else {
            ExtendedWriter datasetWriter = new ExtendedWriter(new File(outputFolderPath, "dataset.json"));
            switch (memoryType) {
                case DISK:
                    dataSetSaver = new FromFileDataSetSaver(new FileDataSetIterator(datasetFilePath), formatter, datasetWriter);
                    break;
                case MEMORY:
                    dataSetSaver = new InMemoryDataSetSaver(formatter, datasetWriter);
                    break;
                case NONE:
                    dataSetSaver = new NoActionDataSetSaver();
                    break;
                default:
                    throw new NotImplementedException();
            }
        }

        builder.setDataSetIterator(new FileDataSetIterator(datasetFilePath));
        builder.setDataSetSaver(dataSetSaver);
        builder.setPriorProbabilityEstimator(estimator);
        builder.setStatisticsAggregator(new BatchesStatisticsAggregator(new ArrayList<>(classificationTable.getGroupsNames()), outputFolderPath));
        builder.setTable(classificationTable);

        builder.build().classify();

        return consumedArguments;
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
