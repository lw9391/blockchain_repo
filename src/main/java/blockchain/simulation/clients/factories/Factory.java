package blockchain.simulation.clients.factories;

import blockchain.core.Block;
import blockchain.core.MinerReward;

public interface Factory {
    Block createNewBlock(MinerReward reward);
}
