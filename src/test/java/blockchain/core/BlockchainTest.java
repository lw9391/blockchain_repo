package blockchain.core;

import blockchain.encryption.AddressGenerator;
import blockchain.encryption.EncryptionUtils;
import blockchain.utils.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

class BlockchainTest {
    private Blockchain blockchain;
    private KeyPair miner;
    private KeyPair client;

    @BeforeEach
    void setUp() throws IOException, ClassNotFoundException {
        blockchain = Blockchain.getInstance();
        miner = (KeyPair) SerializationUtils.deserialize("src/test/resources/saved/pair1");
        client = (KeyPair) SerializationUtils.deserialize("src/test/resources/saved/pair2");
    }

    @Test
    void testAppendNextBlock() {
        Block zeroBlock = blockchain.getLast();
        String minerAddress = AddressGenerator.GenerateAddressFromPublicKey(miner.getPublic().getEncoded());
        String clientAddress = AddressGenerator.GenerateAddressFromPublicKey(client.getPublic().getEncoded());
        /* Valid block data */
        int id = zeroBlock.getId() + 1;
        long timeStamp = new Date().getTime();
        String transactionsHash = EncryptionUtils.hashListOfTransactions(new ArrayList<>());
        String prevHash = zeroBlock.getBlockHash();
        MinerReward minerReward = new MinerReward(minerAddress, 100);
    }


    private Block simpleBlockFactoryMethod(int id, long time, String prevHash, String transactionHash, MinerReward reward) {
        int startingZeros = 5;
        int nonce;
        Random random = new Random();
        int numberOfStartingZeros = blockchain.getNumberOfZerosRequired();
        String newBlockHash;
        do {
            nonce = random.nextInt((int) Math.pow(10, numberOfStartingZeros + 4));
            newBlockHash = EncryptionUtils.applySha256(reward.toString() + id + time + nonce + prevHash + transactionHash);
        } while (!newBlockHash.substring(0, numberOfStartingZeros).equals(startingZeros));

        return null;
    }
}