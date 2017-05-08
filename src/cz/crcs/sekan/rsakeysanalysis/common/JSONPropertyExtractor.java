package cz.crcs.sekan.rsakeysanalysis.common;

import cz.crcs.sekan.rsakeysanalysis.tools.DuplicityRemover;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xnemec1
 * @version 4/27/17.
 */
public class JSONPropertyExtractor {
    private boolean caseSensitive;
    private Pattern pattern;

    public static final Pattern MODULUS_PATTERN = Pattern.compile("\"(n|modulus)\" *: *\"(0x|0X)?([a-zA-Z0-9]+)\"");
    public static final Pattern FINGERPRINT_PATTERN = Pattern.compile("\"fprint\" *: *\"(0x|0X)?([a-zA-Z0-9]+)\"");

    public static final JSONPropertyExtractor MODULUS_CASE_INSENSITIVE_EXTRACTOR =
            new JSONPropertyExtractor(false, MODULUS_PATTERN);
    public static final JSONPropertyExtractor FINGERPRINT_CASE_INSENSITIVE_EXTRACTOR =
            new JSONPropertyExtractor(false, FINGERPRINT_PATTERN);

    public JSONPropertyExtractor(boolean caseSensitive, Pattern pattern) {
        this.caseSensitive = caseSensitive;
        this.pattern = pattern;
    }

    /**
     * Returns the first match of the set pattern
     *
     * @param json json string where to look for the pattern
     * @return first match of the pattern or null
     */
    public String extractProperty(String json) {
        if (!caseSensitive) json = json.toLowerCase();
        Matcher m = pattern.matcher(json);
        if (m.find()) {
            String matched = m.group(0);
            String[] split = matched.split("\"");
            if (split.length >= 4) {
                matched = split[3];
                if (matched.startsWith("0x") || matched.startsWith("0X")) matched = matched.substring(2);
            } else {
                System.err.println("No match in json or malformed pattern");
                return null;
            }
            return matched;
        }
        return null;
    }

    public BigInteger extractHashOfProperty(String json) {
        String property = extractProperty(json);
        if (property == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte hash[] = digest.digest(property.getBytes());
            return new BigInteger(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
