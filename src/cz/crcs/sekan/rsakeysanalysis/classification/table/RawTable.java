package cz.crcs.sekan.rsakeysanalysis.classification.table;

import cz.crcs.sekan.rsakeysanalysis.classification.table.identification.IdentificationGenerator;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.Transformation;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.TransformationNotFoundException;
import cz.crcs.sekan.rsakeysanalysis.classification.table.transformation.exception.WrongTransformationFormatException;
import cz.crcs.sekan.rsakeysanalysis.common.ExtendedWriter;
import cz.crcs.sekan.rsakeysanalysis.common.GroupsComparator;
import javafx.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 20.04.2016
 */
public class RawTable {
    /**
     * Default value for max euclidean distance for create group
     */
    public static final double DEFAULT_MAX_EUCLIDEAN_DISTANCE = 0.02;

    public static final BigDecimal DEFAULT_SOURCE_WEIGHT = BigDecimal.ONE;

    /**
     * Map of sources contains map of identification
     * Source -> Identification -> Count
     */
    private Map<String, Map<String, Long>> table = new TreeMap<>();

    private JSONArray identifications;

    private JSONObject groups;

    private Map<String, BigDecimal> sourceWeights;

    public RawTable(JSONArray identifications, JSONObject groups) {
        this.identifications = identifications;
        this.groups = groups;
        this.sourceWeights = new TreeMap<>();
    }

    public double getMaxEuclideanDistanceForGroup() {
        try {
            return ((Number)groups.get("maxEuclideanDistance")).doubleValue();
        }
        catch (Exception ex) {
            return DEFAULT_MAX_EUCLIDEAN_DISTANCE;
        }
    }

    public ArrayList<String> getRepresentants() {
        JSONArray array = (JSONArray)this.groups.get("representants");
        if (array == null) return null;
        ArrayList<String> representants = new ArrayList<>();
        for (Object representant : array) {
            representants.add((String) representant);
        }
        return representants;
    }

    public synchronized void addSource(String source, Map<String, Long> identificationsCount, BigDecimal sourceWeight) {
        table.put(source, identificationsCount);
        sourceWeights.put(source, sourceWeight);
    }

    public Map<String, Map<String, Long>> getTable() {
        return table;
    }

    public Map<String, Long> computeSourcesCount() {
        Map<String, Long> counts = new TreeMap<>();
        for (Map.Entry<String, Map<String, Long>> entry : table.entrySet()) {
            Long sum = table.get(entry.getKey()).values()
                    .stream()
                    .mapToLong(Long::longValue)
                    .sum();
            counts.put(entry.getKey(), sum);
        }
        return counts;
    }

    public Map<String, Map<String, Double>> computeEuclideanDistances() {
        Map<String, Long> sourcesCount = computeSourcesCount();
        Map<String, Map<String, Double>> correlations = new TreeMap<>();
        for (String X : table.keySet()) {
            Map<String, Double> correlationWithX = new TreeMap<>();
            for (String Y : table.keySet()) {
                double sum = 0.0;
                Set<String> allIdentifications = new HashSet<>();
                allIdentifications.addAll(table.get(X).keySet());
                allIdentifications.addAll(table.get(Y).keySet());

                for (String identification : allIdentifications) {
                    Double valX = 0.0, valY = 0.0;
                    if (table.get(X).containsKey(identification))
                        valX = ((double)table.get(X).get(identification)) / sourcesCount.get(X);
                    if (table.get(Y).containsKey(identification))
                        valY = ((double)table.get(Y).get(identification)) / sourcesCount.get(Y);
                    sum += Math.pow(valX - valY, 2);
                }
                correlationWithX.put(Y, Math.sqrt(sum));
            }
            correlations.put(X, correlationWithX);
        }
        return correlations;
    }

    public Set<Set<String>> computeSourceGroups() {
        Map<String, Map<String, Double>> correlation = computeEuclideanDistances();
        Set<Set<String>> newGroups = new TreeSet<>(new Comparator<Set<String>>(){
            public int compare(Set<String> a, Set<String> b) {
                return a.toString().compareTo(b.toString());
            }
        });

        for (String source : correlation.keySet()) {
            Set<String> newCluster = new TreeSet<>();
            newCluster.add(source);
            newGroups.add(newCluster);
        }

        //Hierarchical clustering
        while (newGroups.size() > 1) {
            Double minDistance = null;
            Pair<Set<String>, Set<String>> minDistancePair = null;
            for (Set<String> cluster : newGroups) {
                for (Set<String> cluster2 : newGroups) {
                    if (cluster == cluster2) continue;

                    Double actualMinDistance = null;
                    for (String clusterSource : cluster) {
                        for (String cluster2Source : cluster2) {
                            Double val = correlation.get(clusterSource).get(cluster2Source);
                            if (actualMinDistance == null || actualMinDistance > val) {
                                actualMinDistance = val;
                            }
                        }
                    }

                    if (minDistance == null || minDistance > actualMinDistance) {
                        minDistance = actualMinDistance;
                        minDistancePair = new Pair<>(cluster, cluster2);
                    }
                }
            }

            if (minDistance != null && minDistance < getMaxEuclideanDistanceForGroup()) {
                boolean removed = false;
                Iterator<Set<String>> iterator = newGroups.iterator();
                while (iterator.hasNext() && !removed) {
                    Set<String> set = iterator.next();
                    if(set.equals(minDistancePair.getValue())) {
                        iterator.remove();
                        removed = true;
                    }
                }
                if (!removed) {
                    throw new RuntimeException("Cannot remove some set during clustering.");
                }

                minDistancePair.getKey().addAll(minDistancePair.getValue());
            }
            else break;
        }

        //Use new sort
        Set<Set<String>> groups = new TreeSet<>(new GroupsComparator(getRepresentants()));
        groups.addAll(newGroups);

        return groups;
    }

    public Map<Set<String>, Map<String, Long>> computeTableGrouped() {
        Set<Set<String>> groups = computeSourceGroups();

        Map<Set<String>, Map<String, Long>> classificationTableGrouped = new TreeMap<>(new GroupsComparator(getRepresentants()));
        for (Set<String> group : groups) {
            Map<String, Long> identificationsCount = new TreeMap<>();
            for (String source : group) {
                for (Map.Entry<String, Long> identificationCount : table.get(source).entrySet()) {
                    Long val = identificationsCount.getOrDefault(identificationCount.getKey(), 0L);
                    val += identificationCount.getValue();
                    identificationsCount.put(identificationCount.getKey(), val);
                }
            }
            classificationTableGrouped.put(group, identificationsCount);
        }
        return classificationTableGrouped;
    }

    public ClassificationTable computeClassificationTable() throws WrongTransformationFormatException, TransformationNotFoundException {
        Map<Set<String>, Map<String, Long>> tableGrouped = computeTableGrouped();

        Map<Set<String>, BigDecimal> groupWeights = new HashMap<>();

        for (Set<String> group : tableGrouped.keySet()) {
            BigDecimal max = BigDecimal.ZERO;

            for (String source : group) {
                BigDecimal weight = sourceWeights.get(source);
                max = max.max(weight == null ? BigDecimal.ZERO : weight);
            }
            Set<String> hashGroup = new HashSet<>();
            hashGroup.addAll(group);
            groupWeights.put(hashGroup, max);
        }

        List<Transformation> transformations = new ArrayList<>();
        for (Object identificationPart : identifications) {
            transformations.add(Transformation.createFromIdentificationPart((JSONObject)identificationPart));
        }
        IdentificationGenerator identificationGenerator = new IdentificationGenerator(transformations);
        return new ClassificationTable(tableGrouped, identificationGenerator, groupWeights);
    }

    public JSONObject toJSONObject() {
        JSONObject root = new JSONObject();

        //Actual date
        DateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        root.put("date", format.format(new Date()));
        //Identifications
        root.put("identifications", identifications);
        //Groups
        root.put("groups", groups);
        //Table
        root.put("table", table);
        root.put("weights", sourceWeights);

        return root;
    }

    public void save(String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            toJSONObject().writeJSONString(writer);
        }
        catch (IOException ex) {
            System.err.println("Cannot save table as json to file '" + fileName + "'");
        }
    }

    public static RawTable load(String fileName) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(fileName)) {
            JSONObject root = (JSONObject)parser.parse(reader);
            JSONArray identifications = (JSONArray) root.get("identifications");
            JSONObject groups = (JSONObject)root.get("groups");
            JSONObject sourceWeightsObject = (JSONObject)root.get("weights");

            RawTable rawTable = new RawTable(identifications, groups);
            rawTable.table = (Map<String, Map<String, Long>>)root.get("table");

            for (String sourceName : rawTable.table.keySet()) {
                Object value = null;
                if (sourceWeightsObject != null) {
                    value = sourceWeightsObject.get(sourceName);
                }

                if (value == null) {
                    rawTable.sourceWeights.put(sourceName, DEFAULT_SOURCE_WEIGHT);
                } else if (value instanceof Long) {
                    rawTable.sourceWeights.put(sourceName, BigDecimal.valueOf((Long) value));
                } else if (value instanceof Double) {
                    rawTable.sourceWeights.put(sourceName, BigDecimal.valueOf((Double) value));
                } else {
                    rawTable.sourceWeights.put(sourceName, (BigDecimal) value);
                }
            }
            return rawTable;
        }
    }

    public Map<String, Map<String, Long>> copyTable() {
        Map<String, Map<String, Long>> tableTemp = new TreeMap<>();
        for (Map.Entry<String, Map<String, Long>> entrySource : table.entrySet()) {
            Map<String, Long> identificationsCount = new TreeMap<>();
            for (Map.Entry<String, Long> entryIdentification : entrySource.getValue().entrySet()) {
                identificationsCount.put(entryIdentification.getKey(), new Long(entryIdentification.getValue()));
            }
            tableTemp.put(entrySource.getKey(), identificationsCount);
        }
        return tableTemp;
    }

    public Pair<RawTable, RawTable> splitForTests(long numberOfKeysFromSource) throws NoSuchAlgorithmException {
        RawTable testsKeysTable = new RawTable(identifications, groups);
        RawTable withoutTestsKeysTable = new RawTable(identifications, groups);

        //Copy table
        Map<String, Map<String, Long>> tableTemp = copyTable();

        //Remove keys for each source
        Random generator = SecureRandom.getInstance("SHA1PRNG");
        for (String source : table.keySet()) {
            Map<String, Long> identificationsCount = new TreeMap<>();
            long numOfKeys = numberOfKeysFromSource;
            int sumOfKeys = (int)tableTemp.get(source).values().stream().mapToLong(Long::longValue).sum();
            if (sumOfKeys < numOfKeys) {
                throw new RuntimeException("In table is less then " + numOfKeys + " keys (" + sumOfKeys + ").");
            }
            while (numOfKeys > 0) {
                String randomIdentification = null;
                int keyPos = generator.nextInt(sumOfKeys);
                for (Map.Entry<String, Long> entry : tableTemp.get(source).entrySet()) {
                    if (keyPos < entry.getValue()) {
                        randomIdentification = entry.getKey();
                        break;
                    }
                    keyPos -= entry.getValue();
                }
                if (randomIdentification == null) {
                    throw new RuntimeException("Cannot get random key from table.");
                }
                Long keys = tableTemp.get(source).get(randomIdentification);
                if (keys == 1) {
                    tableTemp.get(source).remove(randomIdentification);
                }
                else {
                    tableTemp.get(source).put(randomIdentification, keys - 1);
                }

                long val = identificationsCount.getOrDefault(randomIdentification, 0L);
                identificationsCount.put(randomIdentification, val + 1);

                sumOfKeys--;
                numOfKeys--;
            }

            testsKeysTable.addSource(source, identificationsCount, RawTable.DEFAULT_SOURCE_WEIGHT);
        }

        //Add others to another table
        withoutTestsKeysTable.table = tableTemp;

        return new Pair<>(testsKeysTable, withoutTestsKeysTable);
    }

    public void exportToCsvFormat(String outFile) throws IOException {
        Set<String> identifications = new TreeSet<>();
        for (Map<String, Long> map : table.values()) {
            identifications.addAll(map.keySet());
        }

        try (ExtendedWriter writer = new ExtendedWriter(outFile)) {
            writer.writeln("," + String.join(",", table.keySet()));
            for (String identification : identifications) {
                writer.write(identification);
                for (String source : table.keySet()) {
                    Long value = table.get(source).get(identification);
                    writer.write("," + (value == null ? "0" : value.toString()));
                }
                writer.newLine();
            }
        }
    }
}
