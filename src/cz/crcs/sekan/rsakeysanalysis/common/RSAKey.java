package cz.crcs.sekan.rsakeysanalysis.common;

import cz.crcs.sekan.rsakeysanalysis.common.exception.WrongKeyException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @author David Formanek
 * @author Peter Sekan, peter.sekan@mail.muni.cz
 */
public class RSAKey {
    /**
     * Parts of rsa key
     */
    public enum PART {
        N, E, D, P, PMO, PPO, Q, QMO, QPO, PHI, NBLEN, PBLEN, QBLEN
    }

    private static final int RADIX = 10;
    private static final int PRIME_CERTAINITY = 40;

    private BigInteger exponent = null;
    private BigInteger modulus = null;
    private BigInteger p = null;
    private BigInteger q = null;
    private long time = 0;
    private boolean checkedValidity = false;
    private boolean validKey = true;

    public RSAKey() {

    }

    public RSAKey(String modulus, long exponent) {
        this(modulus, BigInteger.valueOf(exponent));
    }

    public RSAKey(String modulus, BigInteger exponent) {
        this(stringToBigInteger(modulus), exponent);
    }

    public RSAKey(BigInteger modulus, BigInteger exponent) {
        this.modulus = modulus;
        this.exponent = exponent;
    }

    public RSAKey createReversedPaQ() {
        RSAKey key = new RSAKey(this.modulus, this.exponent);
        key.setP(this.getQ());
        key.setQ(this.getP());
        key.setTime(this.getTime());
        key.validKey = this.validKey;
        key.checkedValidity = this.checkedValidity;
        return key;
    }

    public BigInteger getExponent() {
        return exponent;
    }

    public void setExponent(BigInteger exponent) {
        this.exponent = exponent;
    }

    public BigInteger getModulus() {
        if (modulus == null) {
            if (p != null && q != null) {
                return p.multiply(q);
            }
        }
        return modulus;
    }

    public void setModulus(BigInteger modulus) {
        this.modulus = modulus;
    }

    public BigInteger getP() {
        return p;
    }

    public void setP(BigInteger p) {
        this.p = p;
    }

    public BigInteger getQ() {
        return q;
    }

    public void setQ(BigInteger q) {
        this.q = q;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isValidKey() {
        return isValid(false);
    }

    public boolean isValid(boolean writeInfo) {
        if (checkedValidity)
            return validKey;

        boolean isValid = true;
        if (!p.isProbablePrime(PRIME_CERTAINITY)) {
            isValid = false;
            if (writeInfo) {
                System.out.println("p " + p.toString(RADIX) + " is not a prime");
            }
        }
        if (!q.isProbablePrime(PRIME_CERTAINITY)) {
            isValid = false;
            if (writeInfo) {
                System.out.println("q " + q.toString(RADIX) + " is not a prime");
            }
        }
        if (!p.multiply(q).equals(modulus)) {
            isValid = false;
            if (writeInfo) {
                System.out.println("Modulus " + modulus.toString(RADIX) + " has not factors p a q");
            }
        }
        BigInteger phi = getPhi();
        if (!phi.gcd(exponent).equals(BigInteger.ONE)) {
            isValid = false;
            if (writeInfo) {
                System.out.println("Exponent " + exponent.toString(RADIX)
                        + " is not coprime to phi of " + modulus.toString(RADIX));
            }
        }
        checkedValidity = true;
        validKey = isValid;
        return isValid;
    }

    public BigInteger getPrimeDifference() {
        return p.subtract(q).abs();
    }

    public BigInteger getPrivateExponent() {
        BigInteger phi = getPhi();
        return exponent.modInverse(phi);
    }

    public BigInteger getPhi() {
        return modulus.subtract(p).subtract(q).add(BigInteger.ONE);
    }

    public BigInteger getPart(PART part) {
        switch (part) {
            case N:
                return getModulus();
            case E:
                return getExponent();
            case D:
                return getPrivateExponent();
            case P:
                return getP();
            case PMO:
                if (getP() == null) return null;
                return getP().subtract(BigInteger.ONE);
            case PPO:
                if (getP() == null) return null;
                return getP().add(BigInteger.ONE);
            case Q:
                return getQ();
            case QMO:
                if (getQ() == null) return null;
                return getQ().subtract(BigInteger.ONE);
            case QPO:
                if (getQ() == null) return null;
                return getQ().add(BigInteger.ONE);
            case PHI:
                return getPhi();
            case NBLEN:
                if (getModulus() == null) return null;
                return BigInteger.valueOf(getModulus().bitLength());
            case PBLEN:
                if (getP() == null) return null;
                return BigInteger.valueOf(getP().bitLength());
            case QBLEN:
                if (getQ() == null) return null;
                return BigInteger.valueOf(getQ().bitLength());
            default:
                throw new IllegalArgumentException("Unknown part of RSAKey: '" + part + "'");
        }
    }

    public static RSAKey readPublicKeyFromTlv(String value) throws WrongKeyException {
        RSAKey rsaKey = new RSAKey();
        BigInteger[] values = parseTlv(value);
        if (values.length != 2) {
            throw new WrongKeyException("Public key " + value + " not composed from 2 values");
        }
        rsaKey.setExponent(values[0]);
        rsaKey.setModulus(values[1]);
        return rsaKey;
    }

    public void readPrivateKeyFromTlv(String value) throws WrongKeyException {
        BigInteger[] values = parseTlv(value);
        if (values.length != 2) {
            throw new WrongKeyException("Private key " + value + "  not composed from 2 values");
        }
        setP(values[0]);
        setQ(values[1]);
    }

    private static BigInteger[] parseTlv(String values) throws WrongKeyException {
        int totalLength = values.length();
        if (totalLength % 2 == 1) {
            throw new WrongKeyException("Odd String length of " + values);
        }
        List<BigInteger> result = new ArrayList<>();
        int length;
        for (int offset = 0; offset != totalLength; offset += 6 + 2 * length) {
            if (offset > totalLength) {
                throw new WrongKeyException("Invalid TLV length in " + values);
            }
            if (!values.startsWith("82", offset)) {
                throw new WrongKeyException("Invalid TLV type in " + values);
            }
            length = Integer.parseInt(values.substring(offset + 2, offset + 6), 16);
            String value = values.substring(offset + 6, offset + 6 + 2 * length);
            result.add(new BigInteger(value, 16));
        }
        return result.toArray(new BigInteger[result.size()]);
    }

    public void writeToFile(BufferedWriter writer, long keyNumber) throws IOException {
        writer.write(Long.toString(keyNumber));
        writer.write(';');
        writer.write(modulus.toString(16).toUpperCase());
        writer.write(';');
        writer.write(exponent.toString(16).toUpperCase());
        writer.write(';');
        writer.write(p.toString(16).toUpperCase());
        writer.write(';');
        writer.write(q.toString(16).toUpperCase());
        writer.write(';');
        writer.write(getPrivateExponent().toString(16).toUpperCase());
        writer.write(';');
        writer.write(Long.toString(time));
        writer.newLine();
    }

    @Override
    public String toString() {
        return "RSAKey{" +
                "exponent=" + exponent +
                ", modulus=" + modulus +
                ", p=" + p +
                ", q=" + q +
                ", time=" + time +
                '}';
    }

    /**
     * Parse string to key
     * @param text parsing text
     * @return loaded key | null if text not contains key
     */
    public static RSAKey loadFromString(String text) throws WrongKeyException {
        String tuple[] = text.replace(",", ";").split(";", 7);
        if (tuple.length != 7 || !tuple[0].matches("\\d+")) {
            return null;
        }

        try {
            RSAKey rsaKey = new RSAKey();
            rsaKey.setModulus(new BigInteger(tuple[1], 16));
            rsaKey.setExponent(new BigInteger(tuple[2], 16));
            rsaKey.setP(new BigInteger(tuple[3], 16));
            rsaKey.setQ(new BigInteger(tuple[4], 16));
            rsaKey.setTime(Long.valueOf(tuple[6]));
            return rsaKey;
        } catch (NumberFormatException ex) {
            throw new WrongKeyException(ex);
        }
    }

    public static BigInteger stringToBigInteger(String number) {
        number = number.replaceAll("\\s","");
        String binary;
        if (number.matches("^([0-1]*)$")) {
            binary = number;
        }
        else if (number.matches("^([A-Fa-f0-9]*)$")) {
            BigInteger bi = new BigInteger(number, 16);
            binary = bi.toString(2);
        }
        else if (number.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$")) {
            binary = base64ToBinary(number);
        }
        else throw new IllegalArgumentException("Number '" + number + "' is not in binary, hexadecimal or in base64.");
        return new BigInteger(binary, 2);
    }

    private static String base64ToBinary(String base64) {
        byte arr[] = Base64.getDecoder().decode(base64);
        String tmp = "";
        for (byte b : arr) {
            tmp += Integer.toBinaryString(b & 255 | 256).substring(1);
        }
        return tmp;
    }
}
