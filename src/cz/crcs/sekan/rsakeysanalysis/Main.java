package cz.crcs.sekan.rsakeysanalysis;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.makefile.Makefile;
import cz.crcs.sekan.rsakeysanalysis.classification.table.RawTable;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.ClassificationSuccess;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.Misclassification;
import cz.crcs.sekan.rsakeysanalysis.classification.tests.ModulusFactors;
import cz.crcs.sekan.rsakeysanalysis.common.*;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import cz.crcs.sekan.rsakeysanalysis.tools.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
                    classifyDataSet(classificationTable, args[++i], args[++i]);
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
                "                        out   = path to folder for store results\n" +
                "  -rd  in    out       Remove duplicity from key set.\n" +
                "                        in    = path to key set\n" +
                "                        out   = path to key set\n" +
                "  -nc  table           Set classification table for not classify some keys.\n" +
                "                        table = path to classification table file\n" +
                "  -h                   Show this help.\n" +
                "" +
                "");
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
     */
    private static void classifyDataSet(ClassificationTable table, String fileName, String folder) {
        if (table == null) throw new IllegalArgumentException("Table in function classifyDataSet cannot be null.");
        if (fileName == null) throw new IllegalArgumentException("File name in function classifyDataSet cannot be null.");
        if (folder == null) throw new IllegalArgumentException("Folder name in function classifyDataSet cannot be null.");

        //Create folder for results if does not exist
        File folderFile = new File(folder);
        if (!folderFile.exists()) {
            if (!folderFile.mkdirs()) {
                throw new IllegalArgumentException("Cannot create folder.");
            }
        }

        //Check if path to folder is correctly ended
        if (!folder.endsWith("/") && !folder.endsWith("\\")) {
            folder += "/";
        }

        //Print information about groups in classification table
        System.out.println("Groups:");
        for (String groupName : table.getGroupsNames()) {
            System.out.println("\t" + groupName + ": " + String.join(", ", table.getGroupSources(groupName)));
        }
        System.out.println();

        JSONParser parser = new JSONParser();
        String subjectId = "common_name";

        //Container of results: subject identification -> date identification -> classification container
        Map<String, Map<String, ClassificationContainer>> parsedData = new HashMap<>();

        //Read and parse all certificates
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String jsonLine;
            long line = 0;
            while ((jsonLine = reader.readLine()) != null) {
                line++;
                //Count the lines and each 100000 print actual line
                if (line % 100000 == 0) System.out.println("Parsed " + line + " lines.");

                //Skip blank lines
                if (jsonLine.length() == 0) continue;

                try {
                    //Check if certificate has a valid json format (rsa_public_key, subject and validity property is needed)
                    JSONObject obj = (JSONObject) parser.parse(jsonLine);
                    if (!obj.containsKey("rsa_public_key") ||
                            !obj.containsKey("subject") ||
                            !obj.containsKey("validity"))
                        continue;

                    //Read all needed information about certificate
                    //Property count is not necessary, represent number of duplicities in source key set
                    Number countNumber = 1;
                    if (obj.containsKey("count"))countNumber = (Number) obj.get("count");
                    long count = countNumber.longValue();

                    //Property validity has to have property start with date
                    //If date has W3C date and time format or similar, only date is extracted
                    JSONObject validity = (JSONObject) obj.get("validity");
                    String validityStart = (String) validity.get("start");
                    String validityStartByDay = validityStart;
                    if (validityStart.contains("T")) {
                        validityStartByDay = validityStart.split("T")[0];
                    }

                    //Property rsa_public_key has to have properties modulus and exponent
                    JSONObject rsa_public_key = (JSONObject) obj.get("rsa_public_key");
                    String modulus = (String) rsa_public_key.get("modulus");
                    Object exponentObject = rsa_public_key.get("exponent");
                    BigInteger exponent;
                    try {
                        exponent = BigInteger.valueOf(((Number)exponentObject).longValue());
                    }
                    catch (ClassCastException ex) {
                        exponent = new BigInteger((String)exponentObject, 16);
                    }

                    //Property subject has to have property common_name
                    JSONObject subject = (JSONObject) obj.get("subject");
                    if (!subject.containsKey(subjectId)) continue;
                    String subjectIdValue = subject.get(subjectId).toString();

                    //Create public key object
                    RSAKey key = new RSAKey(modulus, exponent);

                    //Check for modulus factors (p or q)
                    if (modulusFactors != null) {
                        BigInteger factor = modulusFactors.getFactor(key);
                        if (factor != null) {
                            System.out.println("FACTOR: Line: " + line + ", Factor: 0x" + factor.toString(16) + ", RSAKey: " + key + ", Subject: " + subjectIdValue);
                        }
                    }

                    //Check if key would not be classify
                    if (classificationTableForNotClassify != null) {
                        if (classificationTableForNotClassify.classifyKey(key) == null) {
                            System.out.println("NC: '" + table.generationIdentification(key) + "', Key: " + key);
                            continue;
                        }
                    }

                    //Classify key
                    ClassificationRow row = table.classifyKey(key);
                    if (row == null) {
                        System.err.println("Classification table does not contain identification '" + table.generationIdentification(key) + "', Key: " + key);
                        continue;
                    }

                    //Create new batch by subject identification if does not exist
                    Map<String, ClassificationContainer> mapByDate = parsedData.get(subjectIdValue);
                    if (mapByDate == null) {
                        mapByDate = new HashMap<>();
                        parsedData.put(subjectIdValue, mapByDate);
                    }

                    //Create new batch by date identification if does not exist
                    ClassificationContainer container = mapByDate.get(validityStartByDay);
                    if (container == null) {
                        mapByDate.put(validityStartByDay, new ClassificationContainer(count, row));
                    }
                    else {
                        //If batch exists add classified row to container
                        container.add(count, row);
                    }
                } catch (Exception ex) {
                    System.err.println("Error: cannot parse line " + line + ": " + ex.getMessage());
                    System.err.println("Json line: " + jsonLine);
                }
            }
        } catch (IOException ex) {
            System.err.println("Error while reading file '" + fileName + "'.");
        }


        // Create containers for statistics
        Long minKeys[] = {1L, 2L, 10L, 100L};
        Map<Long, ExtendedWriter> outputsAll = new HashMap<>();
        Map<Long, Long> groupsCount = new HashMap<>();
        Map<Long, Long> groupsCountUnique = new HashMap<>();
        Map<Long, Map<String, BigDecimal>> groupsPositiveCountAll = new HashMap<>();
        Map<Long, Map<String, BigDecimal>> groupsPositiveCountUniqueAll = new HashMap<>();
        Map<Long, Map<String, Long>> groupsNegativeCountAll = new HashMap<>();
        Map<Long, Map<String, Long>> groupsNegativeCountUniqueAll = new HashMap<>();

        //Initialize containers
        for (int i = 0; i < minKeys.length; i++) {
            Long minKey = minKeys[i];
            groupsCount.put(minKey, 0L);
            groupsCountUnique.put(minKey, 0L);

            String file = "allResults - " + subjectId + " - sameDay - " + minKey + " keys for classification.csv";
            try {
                ExtendedWriter writer = new ExtendedWriter(folder + file);
                outputsAll.put(minKey, writer);
            } catch (IOException ex) {
                System.err.println("Error while opening file '" + file + "' for results.");
            }

            Map<String, BigDecimal> groupsPositiveCount = new TreeMap<>();
            Map<String, BigDecimal> groupsPositiveCountUnique = new TreeMap<>();
            Map<String, Long> groupsNegativeCount = new TreeMap<>();
            Map<String, Long> groupsNegativeCountUnique = new TreeMap<>();
            for (String groupName : table.getGroupsNames()) {
                groupsPositiveCount.put(groupName, BigDecimal.ZERO);
                groupsPositiveCountUnique.put(groupName, BigDecimal.ZERO);
                groupsNegativeCount.put(groupName, 0L);
                groupsNegativeCountUnique.put(groupName, 0L);
            }
            groupsPositiveCountAll.put(minKey, groupsPositiveCount);
            groupsPositiveCountUniqueAll.put(minKey, groupsPositiveCountUnique);
            groupsNegativeCountAll.put(minKey, groupsNegativeCount);
            groupsNegativeCountUniqueAll.put(minKey, groupsNegativeCountUnique);
        }

        //Compute statistics
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
        otherSymbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#0.00000000", otherSymbols);
        for (Map.Entry<String, Map<String, ClassificationContainer>> subject : parsedData.entrySet()) {
            for (Map.Entry<String, ClassificationContainer> date : subject.getValue().entrySet()) {
                ClassificationRow row = date.getValue().getRow();
                row.normalize();
                long allKeys = date.getValue().getNumOfKeys();
                long uniqueKeys = date.getValue().getNumOfRows();

                for (int i = 0; i < minKeys.length; i++) {
                    Long minKey = minKeys[i], maxKey = null;
                    if (i < minKeys.length - 1) maxKey = minKeys[i + 1];

                    ExtendedWriter writer = outputsAll.get(minKey);
                    if (date.getValue().getNumOfRows() < minKey || writer == null) continue;
                    if (maxKey != null) {
                        if (date.getValue().getNumOfRows() >= maxKey) continue;
                    }

                    groupsCount.put(minKey, groupsCount.get(minKey) + date.getValue().getNumOfKeys());
                    groupsCountUnique.put(minKey, groupsCountUnique.get(minKey) + date.getValue().getNumOfRows());

                    Map<String, BigDecimal> groupsPositiveCount = groupsPositiveCountAll.get(minKey);
                    Map<String, BigDecimal> groupsPositiveCountUnique = groupsPositiveCountUniqueAll.get(minKey);
                    Map<String, Long> groupsNegativeCount = groupsNegativeCountAll.get(minKey);
                    Map<String, Long> groupsNegativeCountUnique = groupsNegativeCountUniqueAll.get(minKey);

                    try {
                        writer.writeln(subjectId + ";" + subject.getKey());
                        writer.writeln("Date;" + date.getKey());
                        writer.writeln("Unique keys;" + uniqueKeys);
                        writer.writeln("Keys;" + allKeys);
                        writer.newLine();

                        String groups = String.join(";", table.getGroupsNames());
                        String values = "";
                        for (String groupName : table.getGroupsNames()) {
                            BigDecimal val = row.getSource(groupName);
                            if (val != null) {
                                values += ";" + formatter.format(val.doubleValue() * 100);
                                groupsPositiveCount.put(groupName, groupsPositiveCount.get(groupName).add(val.multiply(BigDecimal.valueOf(allKeys))));
                                groupsPositiveCountUnique.put(groupName, groupsPositiveCountUnique.get(groupName).add(val.multiply(BigDecimal.valueOf(uniqueKeys))));
                            }
                            else {
                                values += ";-";
                                groupsNegativeCount.put(groupName, groupsNegativeCount.get(groupName) + allKeys);
                                groupsNegativeCountUnique.put(groupName, groupsNegativeCountUnique.get(groupName) + uniqueKeys);
                            }
                        }
                        writer.writeln(groups);
                        writer.writeln(values);
                        writer.newLine();
                        writer.newLine();
                    } catch (IOException ex) {
                        System.err.println("Error while writing result to file.");
                    }
                }
            }
        }

        for (int i = 0; i < minKeys.length; i++) {
            Long minKey = minKeys[i];
            try {
                ExtendedWriter writer = outputsAll.get(minKey);
                if (writer != null) writer.close();
            } catch (IOException ex) {
                System.err.println("Error while closing file for results.");
            }

            String file = "groups - " + subjectId + " - sameDay - " + minKey + " keys for classification.csv";
            try (ExtendedWriter writer = new ExtendedWriter(folder + file)) {
                String groupsPositive = "Group name";
                String valuesPositive = "Key count";
                for (Map.Entry<String, BigDecimal> entry : groupsPositiveCountAll.get(minKey).entrySet()) {
                    groupsPositive += ";" + entry.getKey();
                    if (groupsCount.get(minKey) == 0L) valuesPositive += ";-";
                    else valuesPositive += ";" + formatter.format(entry.getValue().divide(BigDecimal.valueOf(groupsCount.get(minKey)), BigDecimal.ROUND_CEILING).doubleValue());
                }

                String groupsPositiveUnique = "Group name";
                String valuesPositiveUnique = "Key count";
                for (Map.Entry<String, BigDecimal> entry : groupsPositiveCountUniqueAll.get(minKey).entrySet()) {
                    groupsPositiveUnique += ";" + entry.getKey();
                    if (groupsCountUnique.get(minKey) == 0L) valuesPositiveUnique += ";-";
                    else valuesPositiveUnique += ";" + formatter.format(entry.getValue().divide(BigDecimal.valueOf(groupsCountUnique.get(minKey)), BigDecimal.ROUND_CEILING).doubleValue());
                }

                String groupsNegative = "Group name";
                String valuesNegative = "Key count";
                for (Map.Entry<String, Long> entry : groupsNegativeCountAll.get(minKey).entrySet()) {
                    groupsNegative += ";" + entry.getKey();
                    valuesNegative += ";" + entry.getValue();
                }

                String groupsNegativeUnique = "Group name";
                String valuesNegativeUnique = "Key count";
                for (Map.Entry<String, Long> entry : groupsNegativeCountUniqueAll.get(minKey).entrySet()) {
                    groupsNegativeUnique += ";" + entry.getKey();
                    valuesNegativeUnique += ";" + entry.getValue();
                }

                writer.writeln("Keys All;" + groupsCount.get(minKey));
                writer.writeln("Keys Unique;" + groupsCountUnique.get(minKey));
                writer.newLine();

                writer.writeln("Positive");
                writer.writeln(groupsPositive);
                writer.writeln(valuesPositive);
                writer.newLine();

                writer.writeln("Positive;Unique");
                writer.writeln(groupsPositiveUnique);
                writer.writeln(valuesPositiveUnique);
                writer.newLine();

                writer.writeln("Negative");
                writer.writeln(groupsNegative);
                writer.writeln(valuesNegative);
                writer.newLine();

                writer.writeln("Negative;Unique");
                writer.writeln(groupsNegativeUnique);
                writer.writeln(valuesNegativeUnique);
                writer.newLine();
            } catch (IOException ex) {
                System.err.println("Error while writing result to file '" + file + ".csv'.");
            }
        }
    }
}
