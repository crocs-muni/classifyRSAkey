package cz.crcs.sekan.rsakeysanalysis.tools;

import cz.crcs.sekan.rsakeysanalysis.common.RSAKey;
import cz.crcs.sekan.rsakeysanalysis.common.StringLengthComparator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 12.06.2016
 */
public class PgpSetToDataSet {
    public static void run(String infile, String outfile) {
        JSONParser parser = new JSONParser();
        long line = 0, keys = 0, withoutName = 0;

        StringLengthComparator comparator = new StringLengthComparator();
        Map<Long, Long> nblenCount = new TreeMap<>();
        Map<String, Long> eCount = new TreeMap<>(comparator);
        Map<Long, Map<String, Long>> nblenExponentCount = new TreeMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(infile))) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfile))) {
                String jsonLine;
                while ((jsonLine = reader.readLine()) != null) {
                    line++;
                    if (line % 1000000 == 0) System.out.println("Parsed " + line + " lines.");
                    if (jsonLine.length() == 0) continue;

                    try {
                        JSONObject obj = (JSONObject) parser.parse(jsonLine);
                        if (!obj.containsKey("algo_id") ||
                                !obj.containsKey("e") ||
                                !obj.containsKey("n") ||
                                !obj.containsKey("creation_time"))
                            continue;

                        Number creationTimeNumber = (Number) obj.get("creation_time");
                        long creationTime = creationTimeNumber.longValue();
                        String n = (String) obj.get("n");
                        String e = (String) obj.get("e");
                        Number algoIdNumber = (Number) obj.get("algo_id");
                        long algoId = algoIdNumber.longValue();
                        if (algoId != 1 && algoId != 2 && algoId != 3) continue;

                        String userId = null;

                        if (obj.containsKey("packets")) {
                            JSONArray packets = (JSONArray) obj.get("packets");
                            for (Object packet : packets) {
                                JSONObject packetJson = (JSONObject) packet;
                                if (packetJson.containsKey("user_id")) {
                                    userId = (String) packetJson.get("user_id");
                                }
                            }
                        }

                        JSONObject objOut = new JSONObject();
                        objOut.put("count", 1);

                        JSONObject validity = new JSONObject();
                        Date date = new Date(creationTime*1000);
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        validity.put("start", format.format(date));
                        validity.put("end", format.format(date));
                        objOut.put("validity", validity);

                        JSONObject rsa_public_key = new JSONObject();
                        String modulus = n.replaceAll("\\s","");
                        long nblen = RSAKey.stringToBigInteger(modulus).bitLength();
                        rsa_public_key.put("modulus", modulus);
                        rsa_public_key.put("exponent", e);
                        objOut.put("rsa_public_key", rsa_public_key);

                        BigInteger eb = new BigInteger(e, 16);
                        if (eb.compareTo(BigInteger.valueOf(65537)) != 0) {
                            continue;
                        }

                        JSONObject subject = new JSONObject();
                        if (userId != null) {
                            subject.put("common_name", userId);
                        }
                        else {
                            withoutName++;
                            continue;
                        }
                        objOut.put("subject", subject);

                        writer.write(objOut.toJSONString());
                        writer.newLine();
                        keys++;

                        nblenCount.putIfAbsent(nblen, 0L);
                        nblenCount.put(nblen, nblenCount.get(nblen) + 1);
                        eCount.putIfAbsent(e, 0L);
                        eCount.put(e, eCount.get(e) + 1);
                        nblenExponentCount.putIfAbsent(nblen, new TreeMap<>(comparator));
                        nblenExponentCount.get(nblen).putIfAbsent(e, 0L);
                        nblenExponentCount.get(nblen).put(e, nblenExponentCount.get(nblen).get(e) + 1);
                    } catch (Exception ex) {
                        System.err.println("Error: cannot parse line " + line + ": " + ex.getMessage());
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("Error while reading file '" + infile + "'.");
        }
        System.out.println("Successfully converted");
        System.out.println("Lines;" + line);
        System.out.println("Keys converted;" + keys);
        System.out.println("Without name;" + withoutName);
        System.out.println();
        System.out.println("Table of nblen;nblen;count");
        for (Map.Entry<Long, Long> entry : nblenCount.entrySet()) {
            System.out.println(";" + entry.getKey() + ";" + entry.getValue());
        }
        System.out.println();
        System.out.println("Table of exponent;exponent;count");
        for (Map.Entry<String, Long> entry : eCount.entrySet()) {
            System.out.println(";" + entry.getKey() + ";" + entry.getValue());
        }
        System.out.println();
        System.out.println("Table of nblen & exponent;nblen;exponent;count");
        for (Map.Entry<Long, Map<String, Long>> entry : nblenExponentCount.entrySet()) {
            for (Map.Entry<String, Long> entry2 : entry.getValue().entrySet()) {
                System.out.println(";" + entry.getKey() + ";" + entry2.getKey() + ";" + entry2.getValue());
            }
        }
    }
}
