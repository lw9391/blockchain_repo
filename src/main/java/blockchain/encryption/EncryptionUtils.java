package blockchain.encryption;

import blockchain.core.SignedTransaction;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public final class EncryptionUtils {
    /* Hex encoding & decoding */
    public static String encodeIntoHex(byte[] input) {
        StringBuilder output = new StringBuilder();
        for (byte b : input) {
            String hex = Integer.toHexString(0xff & b);
            if(hex.length() == 1) output.append('0');
            output.append(hex);
        }
        return output.toString();
    }

    public static byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

    private static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    private static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }

    /* Sha256 generation */
    public static String applySha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return ConvertHashBytesIntoString(hash);
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String applySha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            return ConvertHashBytesIntoString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String ConvertHashBytesIntoString(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte elem: hash) {
            String hex = Integer.toHexString(0xff & elem);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static byte[] applySha256toBytes(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] applyRIPEMD160(String input) {
        return applyRIPEMD160(input.getBytes());
    }

    public static byte[] applyRIPEMD160(byte[] input) {
        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(input, 0, input.length);
        byte[] hash = new byte[20];
        digest.doFinal(hash, 0);
        return hash;
    }

    public static String hashListOfTransactions(List<SignedTransaction> transactions) {
        String reduced = transactions.stream()
                .map(SignedTransaction::stringForHashing)
                .map(EncryptionUtils::applySha256)
                .reduce((sum, next) -> sum += ("\n" + next))
                .orElse("No transactions");
        return EncryptionUtils.applySha256(reduced);
    }

    /* Checksum is the first 4 bytes of double sha256 hash of whatever is being checkedsum'ed.  */
    public static byte[] generateChecksum(byte[] input) {
        byte[] inputSha256 = applySha256toBytes(input);
        byte[] input2xSha256 = applySha256toBytes(inputSha256);
        return Arrays.copyOfRange(input2xSha256, 0, 4);
    }

    /* Hash160 is combination of Sha256 and RIPEMD160 */
    public static byte[] applyHash160(byte[] input) {
        byte[] inputSha256 = applySha256toBytes(input);
        return applyRIPEMD160(inputSha256);
    }
}
