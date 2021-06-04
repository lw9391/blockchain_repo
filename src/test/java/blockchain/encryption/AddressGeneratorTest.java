package blockchain.encryption;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressGeneratorTest {

    @Test
    void generateAddressFromPublicKey() {
        String exampleKey = "025f81956d5826bad7d30daed2b5c8c98e72046c1ec8323da336445476183fb7ca";
        byte[] key = EncryptionUtils.decodeHexString(exampleKey);
        String addressFromPublicKey = AddressGenerator.GenerateAddressFromPublicKey(key);
        String expected = "19eA3hUfKRt7aZymavdQFXg5EZ6KCVKxr8";
        assertEquals(expected, addressFromPublicKey);
    }
}