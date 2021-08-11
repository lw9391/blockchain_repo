package blockchain.core;

import blockchain.encryption.EncryptionUtils;
import blockchain.serialization.BlockSerializer;
import blockchain.serialization.TransactionSerializer;
import blockchain.utils.SerializationUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/* Singleton */
public final class Blockchain {
    private static final Blockchain blockChain = new Blockchain();

    private LinkedList<Block> createdBlocks;
    private TransactionsManager transactionsManager;
    private DifficultyAdjuster difficultyAdjuster;

    public static final long REWARD_VALUE = 100;
    public static final String KEYS_ALGORITHM = "RSA";
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private static final Logger LOGGER = LoggerFactory.getLogger(Blockchain.class);

    private Blockchain() {
        this.createdBlocks = new LinkedList<>();
        Block zeroBlock = Block.newBuilder()
                .setId(0)
                .setTimestamp(0)
                .setHash("0")
                .setMinerName("")
                .setTransactions(new ArrayList<>())
                .setMinerReward(null)
                .build();
        createdBlocks.add(zeroBlock);
        this.transactionsManager = new TransactionsManager(this);
        this.difficultyAdjuster = new DifficultyAdjuster();
    }

    public static synchronized Blockchain getInstance() {
        return blockChain;
    }

    public synchronized boolean appendNextBlock(Block nextBlock) {
        if (!checkIfNextBlockIsValid(nextBlock)) {
            return false;
        }
        LOGGER.info(nextBlock.toString());
        transactionsManager.removeTransactionsAddedInNewBlock(nextBlock);

        boolean appended = createdBlocks.add(nextBlock);
        difficultyAdjuster.adjustDifficulty(createdBlocks);
        System.out.println();
        return appended;
    }

    private synchronized boolean checkIfNextBlockIsValid(Block nextBlock) {
        boolean idValidity = nextBlock.getId() == size() + 1;
        boolean blockBaseValidity = checkBaseBlockData(nextBlock);
        boolean transactionsValidity = transactionsManager.checkNewBlockTransactions(nextBlock);
        return idValidity && blockBaseValidity && transactionsValidity;
    }

    private boolean checkBaseBlockData(Block nextBlock) {
        Block prevBlock = createdBlocks.get(nextBlock.getId() - 1);

        /* Time check (timestamp of received block cant be lower than current moment)
         * This simple condition wouldn't work well in real blockchain but its enough for simulation purposes*/
        long currentTime = new Date().getTime();
        boolean timeCheck = currentTime > nextBlock.getTimestamp();

        /* Previous hash check */
        String prevHash = prevBlock.getBlockHash();
        boolean prevHashCheck = nextBlock.getPreviousBlockHash().equals(prevHash);

        /* Checks if new block hash starts with required number of zeros */
        String startingHash = nextBlock.getBlockHash()
                .substring(0, difficultyAdjuster.getDifficultyValue());
        String startingZerosRequired = "0".repeat(Math.max(0, difficultyAdjuster.getDifficultyValue()));
        boolean zerosCheck = startingHash.equals(startingZerosRequired);

        /* Check transactions hash */
        String transactionsHash = EncryptionUtils.hashListOfTransactions(nextBlock.getTransactions());
        boolean transactionHashCheck = nextBlock.getTransactionsHash().equals(transactionsHash);

        /* Check if hash is correct */
        String newHash = EncryptionUtils
                .applySha256(nextBlock.getMinerReward().toString() + nextBlock.getId() + nextBlock.getTimestamp() + nextBlock.getNonce() + prevHash + transactionsHash);
        boolean hashCheck = newHash.equals(nextBlock.getBlockHash());

        return timeCheck && prevHashCheck && zerosCheck && transactionHashCheck && hashCheck;
    }

    public synchronized boolean addTransaction(SignedTransaction signedTransaction) {
        return transactionsManager.addTransaction(signedTransaction);
    }

    public synchronized long coinsOfClient(String client) {
        return transactionsManager.coinsOfClient(client);
    }

    public synchronized List<SignedTransaction> getTransactionsToPublish() {
        return transactionsManager.getPendingTransactions();
    }

    public synchronized Block getLast() {
        return createdBlocks.getLast();
    }

    public int getNumberOfZerosRequired() {
        return difficultyAdjuster.getDifficultyValue();
    }

    public synchronized int size() {
        return createdBlocks.size() - 1;
    }

    public void saveBlockchainContent(String blocksPath) {
        try {
            SerializationUtils.serialize(createdBlocks, blocksPath);
        } catch (IOException e) {
            LOGGER.error("Error saving Blockchain content, path" + blocksPath, e);
        }
    }

    public void savePendingTransactions(String pendingTransactionsPath) {
        try {
            SerializationUtils.serialize(transactionsManager, pendingTransactionsPath);
        } catch (IOException e) {
            LOGGER.error("Error saving transactions, path" + pendingTransactionsPath, e);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadBlockchainContent(String blocksPath) {
        try {
            createdBlocks = (LinkedList<Block>) SerializationUtils.deserialize(blocksPath);
            difficultyAdjuster = new DifficultyAdjuster();
            difficultyAdjuster.calculateCurrentDifficulty(createdBlocks);
        } catch (IOException e) {
            LOGGER.error("Error loading Blockchain content, path" + blocksPath);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class exception", e);
            throw new RuntimeException(e);
        }
    }

    public void loadPendingTransactions(String pendingTransactionsPath) {
        try {
            this.transactionsManager = (TransactionsManager) SerializationUtils.deserialize(pendingTransactionsPath);
        } catch (IOException e) {
            LOGGER.error("Error loading transactions content, path" + pendingTransactionsPath);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class exception", e);
        }
    }

    public void saveBlockchainAsJson(String path) {
        Gson gson = new GsonBuilder().setPrettyPrinting()
                .registerTypeAdapter(Block.class, new BlockSerializer())
                .registerTypeAdapter(SignedTransaction.class, new TransactionSerializer())
                .create();
        Block[] array = createdBlocks.toArray(new Block[0]);
        String json = gson.toJson(array);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(json);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    List<Block> getBlockList() {
        return new ArrayList<>(createdBlocks);
    }
}