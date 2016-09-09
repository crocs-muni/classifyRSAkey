package cz.crcs.sekan.rsakeysanalysis.classification.table.makefile;

import cz.crcs.sekan.rsakeysanalysis.classification.table.RawTable;
import cz.crcs.sekan.rsakeysanalysis.classification.table.identification.IdentificationGenerator;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.Transformation;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.TransformationNotFoundException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongTransformationFormatException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.makefile.exception.WrongMakefileFormatException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 20.04.2016
 */
public class Makefile {
    /**
     * DataSet
     */
    private Map<String, Set<String>> dataSet = new TreeMap<>();

    /**
     * Identification generator
     */
    private IdentificationGenerator identificationGenerator;

    /**
     * Array of identifications
     */
    private JSONArray identifications;

    /**
     * Setting for groups
     */
    private JSONObject groups;

    /**
     * Number of threads used for parsing dataSet
     */
    private int maxThreads = 3;

    /**
     * Check for keys validity
     */
    private boolean checkForKeysValidity = false;

    /**
     * Parse makefile
     * @param filePath path to makefile
     * @throws IOException
     * @throws ParseException
     * @throws WrongMakefileFormatException
     * @throws WrongTransformationFormatException
     * @throws TransformationNotFoundException
     */
    public Makefile(String filePath) throws IOException, ParseException, WrongMakefileFormatException, WrongTransformationFormatException, TransformationNotFoundException {
        JSONParser parser = new JSONParser();
        File file = new File(filePath);
        String absolutePath = file.getAbsolutePath();
        String baseFolder = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
        try (FileReader read = new FileReader(filePath)) {
            JSONObject makefile = (JSONObject)parser.parse(read);

            //Number of threads
            if (makefile.containsKey("maxThreadsForParsing")) {
                maxThreads = ((Number)makefile.get("maxThreadsForParsing")).intValue();
            }

            //Checks
            if (makefile.containsKey("checks")) {
                for (Object obj : (JSONArray)makefile.get("checks")) {
                    String check = (String)obj;
                    switch (check) {
                        case "keysValidity":
                            checkForKeysValidity = true;
                            break;
                        default:
                            throw new WrongMakefileFormatException("Check '" + check + "' is not defined.");
                    }
                }
            }

            //Groups
            if (makefile.containsKey("groups")) {
                groups = (JSONObject)makefile.get("groups");
            }

            //Identifications
            if (!makefile.containsKey("identifications") || ((JSONArray)makefile.get("identifications")).size() == 0) {
                throw new WrongMakefileFormatException("Parameter 'identifications' is needed and cannot be empty.");
            }
            identifications = (JSONArray)makefile.get("identifications");

            List<Transformation> transformations = new ArrayList<>();
            for (Object identificationPart : identifications) {
                transformations.add(Transformation.createFromIdentificationPart((JSONObject)identificationPart));
            }
            identificationGenerator = new IdentificationGenerator(transformations);

            //DataSet
            if (!makefile.containsKey("dataset")) {
                throw new WrongMakefileFormatException("Parameter 'dataset' is needed.");
            }
            JSONObject dataSets = (JSONObject)makefile.get("dataset");
            for (Object source : dataSets.keySet()) {
                Set<String> sourceDirectories = new HashSet<>();
                JSONArray directories = (JSONArray)dataSets.get(source);
                for (Object directory : directories) {
                    sourceDirectories.add(baseFolder + "/" + directory);
                }
                dataSet.put((String)source, sourceDirectories);
            }
        }
    }

    /**
     * Make raw table
     * @return raw table
     * @throws InterruptedException
     */
    public RawTable make() throws InterruptedException {
        RawTable rawTable = new RawTable(identifications, groups);

        ArrayDeque<SourceParser> waitingThreads = new ArrayDeque<>();
        Set<SourceParser> runningThreads = new HashSet<>();
        for (String source : dataSet.keySet()) {
            //TODO keys validity check
            SourceParser thread = new SourceParser(rawTable, identificationGenerator, source, dataSet.get(source));
            waitingThreads.add(thread);
        }

        while (!waitingThreads.isEmpty() || !runningThreads.isEmpty()) {
            while (runningThreads.size() < maxThreads && !waitingThreads.isEmpty()) {
                SourceParser thread = waitingThreads.pop();
                thread.start();
                runningThreads.add(thread);
            }
            for (SourceParser thread : runningThreads) {
                if (!thread.isAlive()) {
                    runningThreads.remove(thread);
                    break;
                }
            }
            Thread.sleep(1000);
        }

        return rawTable;
    }
}
