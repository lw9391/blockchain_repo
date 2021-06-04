package blockchain.encryption;

import blockchain.core.SignedTransaction;
import blockchain.core.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

class EncryptionUtilsTest {

    @Test
    void testEncodeIntoHex() {
        byte[] example = new byte[] { 10, -11, 12, 14, 45, 16 };
        String exampleHex = "0af50c0e2d10";
        assertEquals(exampleHex, EncryptionUtils.encodeIntoHex(example));
    }

    @Test
    void testDecodeHexString() {
        byte[] example = new byte[] { 10, -11, 12, 14, 45, 16 };
        String exampleHex = "0af50c0e2d10";
        assertArrayEquals(example, EncryptionUtils.decodeHexString(exampleHex));
        String wrongHexString = "1";
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtils.decodeHexString(wrongHexString));
    }

    @Test
    void testApplySha256() {
        String testString = "test";
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", EncryptionUtils.applySha256(testString));

        byte[] testBytes = testString.getBytes();
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", EncryptionUtils.applySha256(testBytes));
    }

    @Test
    void testHashListOfTransactions() {
        byte[] dummyByteArray = new byte[]{0}; //no need to initialize keys and signatures for this test
        List<SignedTransaction> transactions = new ArrayList<>();
        String sender = "sender";
        String receiver = "receiver";
        long amount = 200L;
        long timestampFirst = 1999L;
        long timestampSecond = 2999L;

        /* Empty list check */
        String expectedHashWithEmptyList = "6ca1aa5edf136278a39a5593cdae47d776a5b8d489f23de573eb7c90f6c7f9a0"; //hashed "No transactions"
        assertEquals(expectedHashWithEmptyList, EncryptionUtils.hashListOfTransactions(transactions));

        Transaction first = new Transaction(sender, receiver, amount);
        Transaction second = new Transaction(receiver, sender, amount);
        SignedTransaction firstSigned = new SignedTransaction(first, timestampFirst, dummyByteArray, dummyByteArray);
        SignedTransaction secondSigned = new SignedTransaction(second, timestampSecond, dummyByteArray, dummyByteArray);
        transactions.add(firstSigned);
        transactions.add(secondSigned);
        System.out.println(firstSigned);
        System.out.println(secondSigned);

        String reduced = transactions.stream()
                .map(SignedTransaction::stringForHashing)
                .map(EncryptionUtils::applySha256)
                .reduce((sum, next) -> sum += ("\n" + next))
                .orElse("No transactions");

        System.out.println(reduced);

        String expectedLines = """
                sender sent 200 VC to receiver
                1999
                receiver sent 200 VC to sender
                2999""";
        /* Manually hashed lines above */
        String expectedHashedTransactions = """
                b728093a3a2fd31d3c8d15a90a548212a7038cd1059a8f67e6ed14e76eb17236
                fa7675b4b742fecacc384506abafb3976a57aa5d117ac3b88105dd1597c624c5
                """;
        String expectedHash = "fbc883abd1a427b191972857cc9f8c72c71931c311caa4e777308d0b95ecf8a4"; //manually hashed string above
        assertEquals(expectedHash, EncryptionUtils.hashListOfTransactions(transactions));
    }
}