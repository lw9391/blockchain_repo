package blockchain.encryption;

import blockchain.core.Blockchain;
import org.bitcoinj.core.Base58;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class AddressGenerator {
    private static KeyPairGenerator keyGen;

    public static final int KEY_LENGTH = 1024;
    public static final String ADDRESS_VERSION_NUMBER = "00";

    protected AddressGenerator() {
        super();
    }

    static {
        try {
            keyGen = KeyPairGenerator.getInstance(Blockchain.KEYS_ALGORITHM);
            SecureRandom secureRandom = new SecureRandom();
            keyGen.initialize(KEY_LENGTH, secureRandom);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static KeyPair CreateKeys() {
        return keyGen.generateKeyPair();
    }

    public static String GenerateAddressFromPublicKey(byte[] publicKeyBytes) {
        byte[] hash160ofKey = EncryptionUtils.applyHash160(publicKeyBytes);
        byte[] versionNumber = EncryptionUtils.decodeHexString(ADDRESS_VERSION_NUMBER);

        byte[] keyWithVersion = concatByteArrays(versionNumber, hash160ofKey);
        byte[] checksum = EncryptionUtils.generateChecksum(keyWithVersion);

        byte[] addressBytes = concatByteArrays(keyWithVersion, checksum);

        return Base58.encode(addressBytes);
    }

    private static byte[] concatByteArrays(byte[] first, byte[] second) {
        byte[] concated = new byte[first.length + second.length];
        System.arraycopy(first,0,concated, 0, first.length);
        System.arraycopy(second, 0, concated, first.length, second.length);
        return concated;
    }
}
