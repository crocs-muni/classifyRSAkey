package cz.crcs.sekan.rsakeysanalysis.tools;

import javafx.util.Pair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 * @version 17.06.2016
 */
public class RemoveDuplicity {
    public static void run(String fileIn, String fileOut) {
        JSONParser parser = new JSONParser();

        Map<String, Pair<Long, JSONObject>> uniqueSet = new HashMap<>();
        long lines = 0, allRsaKeys = 0, keys = 0, unique = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(fileIn))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) continue;
                lines++;

                if (lines % 100000 == 0) {
                    System.out.println("Line: " + lines + ", \tAllRSAKeys: " + allRsaKeys + ", \tKeys: " + keys + ", \tUnique: " + unique);
                }

                try {
                    //Check if certificate is in valid json format
                    JSONObject obj = (JSONObject) parser.parse(line);
                    if (!obj.containsKey("rsa_public_key") ||
                            !obj.containsKey("subject") ||
                            !obj.containsKey("validity"))
                        continue;

                    //Read all needed information about certificate
                    Number countNumber = 1;
                    if (obj.containsKey("count"))countNumber = (Number) obj.get("count");
                    long count = countNumber.longValue();
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

                    allRsaKeys++;
                    if (exponent.intValue() == 65537) {
                        Pair<Long, JSONObject> data = uniqueSet.get(modulus);
                        if (data != null) {
                            uniqueSet.put(modulus, new Pair<>(data.getKey() + count, data.getValue()));
                        } else {
                            uniqueSet.put(modulus, new Pair<>(count, obj));
                            unique++;
                        }
                        keys++;
                    }
                } catch (Exception ex) {
                    System.err.println("Error: cannot parse line " + lines + ": " + ex.getMessage());
                }
            }
            System.out.println("Line: " + lines + ", \tAllRSAKeys: " + allRsaKeys + ", \tKeys: " + keys + ", \tUnique: " + unique);
            System.out.println("File '" + fileIn + "' parsed.\n");
        }
        catch (IOException ex) {
            System.err.println("Error while reading file '" + fileIn + "'");
        }


        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileOut))) {
            for (Pair<Long, JSONObject> values : uniqueSet.values()) {
                JSONObject object = values.getValue();
                if (object.containsKey("count")) {
                    object.remove("count");
                }
                object.put("count", values.getKey());

                writer.write(object.toJSONString());
                writer.newLine();
            }
            System.out.println("File '" + fileOut + "' created.\n");
        }
        catch (IOException ex) {
            System.err.println("Error while writing to file: " + ex.getMessage());
        }
    }
}
