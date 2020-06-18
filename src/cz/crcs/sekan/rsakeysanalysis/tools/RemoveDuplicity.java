package cz.crcs.sekan.rsakeysanalysis.tools;

import cz.crcs.sekan.rsakeysanalysis.classification.key.ClassificationKey;

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
        Map<BigInteger, ClassificationKey> uniqueSet = new HashMap<>();
        long lines = 0, keys = 0, unique = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(fileIn))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines++;
                if (line.trim().length() == 0) continue;

                if (lines % 100000 == 0) {
                    System.out.println("Line: " + lines + ", \tKeys: " + keys + ", \tUnique: " + unique);
                }

                try {
                    ClassificationKey key = ClassificationKey.fromJson(line);
                    keys++;
                    BigInteger modulus = key.getRsaKey().getModulus();
                    ClassificationKey data = uniqueSet.get(modulus);
                    if (data != null) {
                        uniqueSet.put(modulus, data.mergeWith(key));
                    } else {
                        uniqueSet.put(modulus, key);
                        unique++;
                    }
                } catch (Exception ex) {
                    System.err.println("Error: cannot parse line " + lines + ": " + ex.getMessage());
                }
            }
            System.out.println("Line: " + lines + ", \tKeys: " + keys + ", \tUnique: " + unique);
            System.out.println("File '" + fileIn + "' parsed.\n");
        }
        catch (IOException ex) {
            System.err.println("Error while reading file '" + fileIn + "'");
        }


        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileOut))) {
            for (ClassificationKey key : uniqueSet.values()) {
                writer.write(key.toJSON().toJSONString());
                writer.newLine();
            }
            System.out.println("File '" + fileOut + "' created.\n");
        }
        catch (IOException ex) {
            System.err.println("Error while writing to file: " + ex.getMessage());
        }
    }
}
