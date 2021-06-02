package blockchain.core;

import blockchain.simulation.clients.Client;
import blockchain.utils.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import static org.junit.jupiter.api.Assertions.*;

class TransactionsManagerTest {
    private Blockchain blockchainExample;
    private String mainerAddress = "1Cm6ByUt4rfZTdwi8Mm4GPCueRS47vz7oS";
    private String clientAddress =  "122VuQdRcG5vRxQ6pKJLuznoXTXBf94Ydm";
    private TransactionsManager transactionsManager;

    @BeforeEach
    void setUp() {
        blockchainExample = Blockchain.getInstance();
        blockchainExample.loadBlockchainContent("src/test/resources/saved/blockchain");
        blockchainExample.loadPendingTransactions("src/test/resources/saved/pending_transactions");
        transactionsManager = blockchainExample.getTransactionsManager();
    }

    @Test
    void testCoinsOfClients() {
        double mainerCoins = transactionsManager.coinsOfClient(mainerAddress);
        double clientCoins = transactionsManager.coinsOfClient(clientAddress);
        double wrongClientCoins = transactionsManager.coinsOfClient("asd");
        /* Miner has 140 coins + 87 frozen in pending transaction */
        /* Client has 73 coins */

        assertEquals(mainerCoins, 140.0);
        assertEquals(clientCoins, 73.0);
        assertEquals(wrongClientCoins, 0.0);
    }

    @Test
    void testAddTransaction() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, ClassNotFoundException {
        Client client = (Client) SerializationUtils.deserialize("src/test/resources/saved/Client 0");
        /*SignedTransaction wrong1 = client.createTransaction(mainerAddress, -5);
        SignedTransaction wrong2 = client.createTransaction(mainerAddress, 0);
        SignedTransaction wrong3 = client.createTransaction(mainerAddress, 75);
        SignedTransaction correct = client.createTransaction(mainerAddress, 70);*/

        /*assertFalse(transactionsManager.addTransaction(wrong1));
        assertFalse(transactionsManager.addTransaction(wrong2));
        assertFalse(transactionsManager.addTransaction(wrong3));
        assertTrue(transactionsManager.addTransaction(correct));*/
    }


}