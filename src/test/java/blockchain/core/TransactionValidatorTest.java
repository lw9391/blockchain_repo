package blockchain.core;

import blockchain.utils.SerializationUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionValidatorTest {
    private static TransactionValidator validator;
    private static String firstClient;
    private static String secondClient;
    private static String thirdClient;
    private static byte[] dummyBytes;

    private List<Block> exampleBlocks;
    private List<SignedTransaction> exampleTransactions;

    @BeforeAll
    static void beforeAll() {
        validator = new TransactionValidator();
        firstClient = "FC";
        secondClient = "SC";
        thirdClient = "TC";
        dummyBytes = new byte[]{1};
    }

    @BeforeEach
    void setUp() {
        exampleBlocks = prepareExampleBlocks();
        exampleTransactions = preparePendingTransactions();
    }

    private List<Block> prepareExampleBlocks() {
        List<Block> blocks = new ArrayList<>(4);

        Block genesis = Block.newBuilder()
                .setId(0)
                .setTimestamp(0)
                .setTransactions(new ArrayList<>())
                .setMinerReward(null)
                .build();
        Block first = Block.newBuilder()
                .setId(1)
                .setMinerReward(new MinerReward(firstClient, 100))
                .setTimestamp(1)
                .setTransactions(new ArrayList<>())
                .build();

        List<SignedTransaction> firstTransactionsList = new ArrayList<>();
        Transaction firstTransaction = new Transaction(firstClient, secondClient, 30);
        firstTransactionsList.add(new SignedTransaction(firstTransaction, 5, dummyBytes, dummyBytes));
        Block second = Block.newBuilder()
                .setId(2)
                .setMinerReward(new MinerReward(secondClient, 100))
                .setTimestamp(10)
                .setTransactions(firstTransactionsList)
                .build();

        blocks.add(genesis);
        blocks.add(first);
        blocks.add(second);
        return blocks;
    }

    private List<SignedTransaction> preparePendingTransactions() {
        List<SignedTransaction> pendingTransactions = new ArrayList<>();
        Transaction first = new Transaction(firstClient, thirdClient, 10);
        SignedTransaction firstSigned = new SignedTransaction(first, 10, dummyBytes, dummyBytes);

        Transaction second = new Transaction(secondClient, thirdClient, 10);
        SignedTransaction secondSigned = new SignedTransaction(second, 12, dummyBytes, dummyBytes);
        pendingTransactions.add(firstSigned);
        pendingTransactions.add(secondSigned);
        return pendingTransactions;
    }

    @Test
    void testCheckTransactionTimeValidity() {
        //Client 1, has later transactions both in blocks and pending list
        Transaction firstClientTransaction = new Transaction(firstClient, secondClient, 30);
        SignedTransaction incorrect = new SignedTransaction(firstClientTransaction, 4, dummyBytes, dummyBytes);
        assertFalse(validator.checkTransactionTimeValidity(incorrect, exampleBlocks, new ArrayList<>()));
        assertFalse(validator.checkTransactionTimeValidity(incorrect, new ArrayList<>(), exampleTransactions));
        assertFalse(validator.checkTransactionTimeValidity(incorrect, exampleBlocks, exampleTransactions));

        //Client 2, has only earlier transactions
        Transaction secondClientTransaction = new Transaction(secondClient, thirdClient, 30);
        SignedTransaction correct = new SignedTransaction(secondClientTransaction, 13, dummyBytes, dummyBytes);
        assertTrue(validator.checkTransactionTimeValidity(correct, exampleBlocks, exampleTransactions));
        assertTrue(validator.checkTransactionTimeValidity(correct, new ArrayList<>(), exampleTransactions));
        assertTrue(validator.checkTransactionTimeValidity(correct, exampleBlocks, new ArrayList<>()));

        //Later than transaction in blocks earlier than transaction in pending list
        SignedTransaction mixed = new SignedTransaction(firstClientTransaction, 7, dummyBytes, dummyBytes);
        assertFalse(validator.checkTransactionTimeValidity(mixed, new ArrayList<>(), exampleTransactions));
        assertTrue(validator.checkTransactionTimeValidity(mixed, exampleBlocks, new ArrayList<>()));
        assertFalse(validator.checkTransactionTimeValidity(mixed, exampleBlocks, exampleTransactions));
    }

    @Test
    void testCoinsOfClient() {
        assertEquals(70, validator.coinsOfClient(firstClient, exampleBlocks, new ArrayList<>()));
        assertEquals(60, validator.coinsOfClient(firstClient, exampleBlocks, exampleTransactions));

        assertEquals(130, validator.coinsOfClient(secondClient, exampleBlocks, new ArrayList<>()));
        assertEquals(120, validator.coinsOfClient(secondClient, exampleBlocks, exampleTransactions));

        assertEquals(0, validator.coinsOfClient(thirdClient, exampleBlocks, new ArrayList<>()));
        assertEquals(0, validator.coinsOfClient(thirdClient, exampleBlocks, exampleTransactions));
    }

    @Test
    void testCheckBalanceValidity() {
        Transaction negativeAmount = new Transaction(firstClient, secondClient, -150);
        SignedTransaction signedNegativeAmount = new SignedTransaction(negativeAmount, 10, dummyBytes, dummyBytes);
        assertFalse(validator.checkBalanceValidity(signedNegativeAmount, exampleBlocks, exampleTransactions));

        Transaction highAmount = new Transaction(firstClient, secondClient, 65);
        SignedTransaction signedHighAmount = new SignedTransaction(highAmount, 10, dummyBytes, dummyBytes);
        assertTrue(validator.checkBalanceValidity(signedHighAmount, exampleBlocks, new ArrayList<>()));
        assertFalse(validator.checkBalanceValidity(signedHighAmount, exampleBlocks, exampleTransactions));
    }

    @Test
    void testCheckSignatureValidity()
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        KeyPair keyPair = (KeyPair) SerializationUtils.deserialize("src/test/resources/pair1");
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        Transaction dummy = new Transaction(firstClient, secondClient, 10);
        long timestamp = 1000L;
        String inputForSignature = dummy.toString() + "\n" + timestamp;

        Signature signature = Signature.getInstance(Blockchain.SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(inputForSignature.getBytes());
        byte[] sign = signature.sign();
        SignedTransaction signedTransaction = new SignedTransaction(dummy, timestamp, sign, publicKey.getEncoded());
        assertTrue(validator.checkSignatureValidity(signedTransaction));
    }
}