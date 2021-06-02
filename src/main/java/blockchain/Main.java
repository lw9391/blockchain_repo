package blockchain;

import blockchain.simulation.BlockchainSimulator;

public class Main {
    public static void main(String[] args) {
        BlockchainSimulator simulator = new BlockchainSimulator();
        simulator.run();
    }
}