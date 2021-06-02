package blockchain.simulation.clients.factories;

import blockchain.core.Block;
import blockchain.core.Blockchain;
import blockchain.core.MinerReward;
import blockchain.core.SignedTransaction;
import blockchain.encryption.EncryptionUtils;

import java.util.Date;
import java.util.List;
import java.util.Random;

public class BlockFactory implements Factory {
    private final Blockchain blockChain;
    private int numberOfStartingZeros;

    public BlockFactory(Blockchain blockChain) {
        this.blockChain = blockChain;
    }

    public Block createNewBlock(MinerReward reward) {
        return generateBlockBuilder(reward).build();
    }

    public Block createNewBlock(MinerReward reward, String minerName) {
        return generateBlockBuilder(reward).setMinerName(minerName).build();
    }

    private Block.Builder generateBlockBuilder(MinerReward reward) {
        List<SignedTransaction> transactions = blockChain.getTransactionsToPublish();
        String transactionHash = EncryptionUtils.hashListOfTransactions(transactions);
        setNumberOfStartingZeros(blockChain.getNumberOfZerosRequired());
        int id = blockChain.getLast().getId() + 1;
        String prevHash = blockChain.getLast()
                .getBlockHash();
        NonceHashTimeWrapper foundNonceHashAndTime = findNonceHashAndTime(id, prevHash, transactionHash, reward);
        return Block.newBuilder()
                .setId(id)
                .setTimestamp(foundNonceHashAndTime.timestamp)
                .setTransactionsHash(transactionHash)
                .setNonce(foundNonceHashAndTime.nonce)
                .setPreviousHash(prevHash)
                .setHash(foundNonceHashAndTime.hash)
                .setTransactions(transactions)
                .setMinerReward(reward);
    }

    private NonceHashTimeWrapper findNonceHashAndTime(int id, String prevHash, String transactionHash, MinerReward reward) {
        String startingZeros = "0".repeat(Math.max(0, numberOfStartingZeros));
        int nonce;
        String newBlockHash;
        Random random = new Random();
        long time = new Date().getTime();
        do {
            nonce = random.nextInt((int) Math.pow(10, numberOfStartingZeros + 4));
            newBlockHash = EncryptionUtils.applySha256(reward.toString() + id + time + nonce + prevHash + transactionHash);
        } while (!newBlockHash.substring(0, numberOfStartingZeros).equals(startingZeros));
        return new NonceHashTimeWrapper(nonce, newBlockHash, time);
    }

    public void setNumberOfStartingZeros(int numberOfStartingZeros) {
        this.numberOfStartingZeros = numberOfStartingZeros;
    }

    private static class NonceHashTimeWrapper {
        private int nonce;
        private String hash;
        private long timestamp;

        private NonceHashTimeWrapper(int nonce, String hash, long timestamp) {
            this.nonce = nonce;
            this.hash = hash;
            this.timestamp = timestamp;
        }
    }
}
