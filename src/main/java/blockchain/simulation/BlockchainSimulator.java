package blockchain.simulation;

import blockchain.core.Blockchain;
import blockchain.simulation.clients.Client;
import blockchain.simulation.clients.Miner;
import blockchain.simulation.config.Configuration;
import blockchain.simulation.config.ConfigurationManager;
import blockchain.utils.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class BlockchainSimulator {
    public static final String CONFIG_FILEPATH = "config/config.json";
    public static final String SERIALIZATION_PATH = "serialization_output/";
    public static final String BLOCKCHAIN_FILENAME = "blockchain";
    public static final String PENDING_TRANSACTIONS_FILENAME = "pending_transactions";
    public static final String MINERS_FILENAME = "miners";
    public static final String CLIENTS_FILENAME = "clients";

    private static ConfigurationManager configurationManager;
    private static Configuration config;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockchainSimulator.class);

    private final Blockchain blockChain;
    private List<Miner> miners;
    private List<Client> clients;
    private final ExecutorService minersThreadPool;
    private final ScheduledExecutorService clientsService;

    public BlockchainSimulator() {
        blockChain = Blockchain.getInstance();
        minersThreadPool = Executors.newFixedThreadPool(config.getNumberOfMiners());
        clientsService = Executors.newScheduledThreadPool(config.getNumberOfClients());
        clients = new ArrayList<>();
        miners = new ArrayList<>();
    }

    static {
        configurationManager = ConfigurationManager.getInstance();
        try {
            loadConfiguration();
            LOGGER.info("Configuration loaded.");
        } catch (FileNotFoundException e) {
            LOGGER.warn("Configuration file not found, using basic configuration.");
            config = configurationManager.getBaseConfiguration();
        }
    }

    private static void loadConfiguration() throws FileNotFoundException {
        configurationManager.loadConfiguration(CONFIG_FILEPATH);
        config = configurationManager.getCurrentConfiguration();
    }

    private void initializeMiners() {
        for (int i = 0; i < config.getNumberOfMiners(); i++) {
            miners.add(new Miner(this, String.valueOf(i)));
        }
    }

    private void initializeClients() {
        int numOfClients = config.getNumberOfClients();
        for (int i = 0; i < numOfClients; i++) {
            clients.add(new Client("Client " + i));
        }
    }

    private void runMinersAndClients() {
        miners.forEach(minersThreadPool::submit);
        Runnable clientsActivity = () -> {
            Random random = new Random();
            int clientId = random.nextInt(config.getNumberOfClients());
            Client sender = clients.get(clientId);

            double balance = Blockchain.getInstance()
                    .coinsOfClient(sender.getAddress());
            if (balance > 0) {
                long toSend = Math.max(1, random.nextInt((int) (balance / 4)));
                sender.sendTransactionToBlockchain(randomClient(sender).getAddress(), toSend);
            }
        };
        clientsService.scheduleWithFixedDelay(clientsActivity, config.getInitialClientsDelay(), config.getClientsTransactionsDelay(), TimeUnit.MILLISECONDS);
    }

    public void run() {
        loadSimulationProgress();
        runMinersAndClients();
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                LOGGER.warn("InterruptedException during waiting", e);
            }
        }
        minersThreadPool.shutdown();
        clientsService.shutdown();

        saveSimulationProgress();
        blockChain.saveBlockchainAsJson(BlockchainSimulator.SERIALIZATION_PATH + BlockchainSimulator.BLOCKCHAIN_FILENAME + ".json");
    }

    public int getAssumedSize() {
        return config.getAssumedBlockchainSize();
    }

    public Client randomClient(Client excluded) {
        List<Client> clientsWithMiners = Stream.concat(clients.stream(), miners.stream().map(Miner::getClient))
                .filter(client -> !client.equals(excluded))
                .collect(Collectors.toList());
        Random random = new Random();
        int nextIndex = random.nextInt(clientsWithMiners.size());
        return clientsWithMiners.get(nextIndex);
    }

    public void saveSimulationProgress() {
        String blocksPath = BlockchainSimulator.SERIALIZATION_PATH + BlockchainSimulator.BLOCKCHAIN_FILENAME;
        String pendingTransactionsPath = BlockchainSimulator.SERIALIZATION_PATH + BlockchainSimulator.PENDING_TRANSACTIONS_FILENAME;
        blockChain.saveBlockchainContent(blocksPath);
        blockChain.savePendingTransactions(pendingTransactionsPath);
        try {
            SerializationUtils.serialize(miners, SERIALIZATION_PATH + MINERS_FILENAME);
            SerializationUtils.serialize(clients, SERIALIZATION_PATH + CLIENTS_FILENAME);
        } catch (IOException e) {
            LOGGER.error("Error saving blockchain progress", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadSimulationProgress() {
        blockChain.loadBlockchainContent(BlockchainSimulator.SERIALIZATION_PATH + BlockchainSimulator.BLOCKCHAIN_FILENAME);
        blockChain.loadPendingTransactions(BlockchainSimulator.SERIALIZATION_PATH + BlockchainSimulator.PENDING_TRANSACTIONS_FILENAME);
        try {
            miners = (ArrayList<Miner>) SerializationUtils.deserialize(SERIALIZATION_PATH + MINERS_FILENAME);
            clients = (ArrayList<Client>) SerializationUtils.deserialize(SERIALIZATION_PATH + CLIENTS_FILENAME);
            miners.forEach(miner -> miner.setSimulator(this));
        } catch (IOException e) {
            LOGGER.warn("Exception occurred during loading miners and clients. Creating new clients and miners...");
            miners.clear();
            clients.clear();
            initializeMiners();
            initializeClients();
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class not found.", e);
            throw new RuntimeException(e);
        }
    }
}