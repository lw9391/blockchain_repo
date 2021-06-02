package blockchain.core;

import java.io.Serializable;

public class MinerReward implements Serializable {
    private final String miner;
    private final long reward;

    public MinerReward(String miner, long reward) {
        this.miner = miner;
        this.reward = reward;
    }

    public String getMiner() {
        return miner;
    }

    public long getReward() {
        return reward;
    }

    @Override
    public String toString() {
        return miner + " gets " + reward + " VC";
    }
}
