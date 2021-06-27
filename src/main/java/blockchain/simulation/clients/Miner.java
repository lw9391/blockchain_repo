package blockchain.simulation.clients;

import blockchain.core.Block;
import blockchain.core.Blockchain;
import blockchain.core.MinerReward;
import blockchain.simulation.clients.factories.BlockFactory;
import blockchain.simulation.BlockchainSimulator;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Random;

public class Miner implements Runnable, Serializable {
    private String minerName;
    private transient BlockFactory factory;
    private transient Blockchain blockChain;
    private transient BlockchainSimulator simulator;
    private Client client;

    public Miner(BlockchainSimulator simulator, String minerName) {
        this.blockChain = Blockchain.getInstance();
        this.factory = new BlockFactory(blockChain);
        this.minerName = minerName;
        this.client = new Client("Miner " + minerName);
        this.simulator = simulator;
    }

    @Override
    public void run() {
        while (simulator.getAssumedSize() > blockChain.size()) {
            MinerReward reward = new MinerReward(client.getAddress(), Blockchain.REWARD_VALUE);
            Block block = factory.createNewBlock(reward, minerName);
            boolean append = blockChain.appendNextBlock(block);
            if (append) {
                prepareTransaction();
                if (block.getId() == simulator.getAssumedSize()) {
                    synchronized (simulator) {
                        simulator.notifyAll();
                    }
                }
            }
        }
    }

    private void prepareTransaction() {
        Random random = new Random();
        int nextInt = random.nextInt(5);
        if (nextInt < 3) {
            int amount = random.nextInt(101);
            Client receiver = simulator.randomClient(client);
            transfer(receiver, amount);
        }
    }

    public void transfer(Client receiver, long amount) {
        client.sendTransactionToBlockchain(receiver.getAddress(), amount);
    }

    public Client getClient() {
        return client;
    }

    public void setSimulator(BlockchainSimulator simulator) {
        this.simulator = simulator;
    }

    private void readObject(ObjectInputStream ois) throws Exception {
        ois.defaultReadObject();
        factory = new BlockFactory(Blockchain.getInstance());
        blockChain = Blockchain.getInstance();
    }
}
