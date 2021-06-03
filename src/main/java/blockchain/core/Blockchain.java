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
    private volatile int numberOfZerosRequired = 5;
    private TransactionsManager transactionsManager;

    public static final long REWARD_VALUE = 100;
    public static final int DIFFICULTY_TARGET = 15;
    public static final int DIFFICULTY_TOLERANCE = 3;
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
        if(appended && nextBlock.getId() % 3 == 0) {
            difficultyAdjustment();
        }
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
                .substring(0, numberOfZerosRequired);
        String startingZerosRequired = "0".repeat(Math.max(0, numberOfZerosRequired));
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

    /* For simulation purposes, blockchain won't increase number of required zeros to more than 6.
    * That time analyze isn't very accurate (specially for that short amounts of time) due to the method of finding nonce -
    * miners gets current time and then try to find nonce with that time(check BlockFactory).
    * Time will be changed only after attempt to add new block */
    private synchronized void difficultyAdjustment() {
        long averageTime = calcAverageCreationTime() / 1000;

        LOGGER.info("Average time creation of 3 last block is " + averageTime + " seconds");
        int difficultyIndex = difficultyCheck(averageTime, numberOfZerosRequired);
        if (difficultyIndex == 1) {
            numberOfZerosRequired++;
            LOGGER.info("N was increased to " + numberOfZerosRequired);
        } else if (difficultyIndex == -1) {
            numberOfZerosRequired--;
            LOGGER.info("N was decreased by 1");
        } else {
            LOGGER.info("N stays the same.");
        }
    }

    private int difficultyCheck(long time, int currentDiff) {
        if (time < (DIFFICULTY_TARGET - DIFFICULTY_TOLERANCE) && currentDiff < 6) {
            return 1;
        } else if (time > (DIFFICULTY_TARGET + DIFFICULTY_TOLERANCE) && currentDiff > 2) {
            return -1;
        } else return 0;
    }

    private synchronized long calcAverageCreationTime() {
        return calcAverageCreationTime(createdBlocks.getLast(), createdBlocks);
    }

    private long calcAverageCreationTime(Block block, List<Block> blockList) {
        int start = block.getId();
        int end = Math.max(start - 3, 1);
        long summedTime = 0;
        for (int i = start; i > end; i--) {
            Block later = blockList.get(i);
            Block earlier = blockList.get(i - 1);
            long compared = later.getTimestamp() - earlier.getTimestamp();
            summedTime += compared;
        }
        return summedTime / (start - end);
    }

     /* Method to estimate difficulty from newly loaded blockchain. Simply getting number of first zero's from
     * last block may on very rare occasions lead to overestimation due to possibility of finding hash with more zeros
     * than is required. To avoid it this method analyze difficulty and time of n blocks. */
    public int calculateDifficulty() {
        ArrayList<Block> arrayList = new ArrayList<>(createdBlocks);
        int difficulty = 5;
        for (int i = 3; i < arrayList.size(); i+=3) {
            Block current = arrayList.get(i);
            long time = calcAverageCreationTime(current, arrayList) / 1000;
            int step = difficultyCheck(time, difficulty);
            difficulty += step;
        }
        return difficulty;
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
        return numberOfZerosRequired;
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
            numberOfZerosRequired = calculateDifficulty();
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