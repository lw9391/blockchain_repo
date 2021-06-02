package blockchain.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class Block implements Serializable {
    private final int id;
    private final long timestamp;
    private final String transactionsHash;
    private final int nonce;
    private final String previousBlockHash;
    private final String blockHash;
    private final String minerName;
    private final MinerReward minerReward;
    private final List<SignedTransaction> transactions;

    private static final long serialVersionUID = 1L;

    protected Block(int id, long timestamp, String transactionsHash, int nonce, String previousBlockHash, String blockHash, String minerName, MinerReward minerReward, List<SignedTransaction> transactions) {
        this.id = id;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.transactionsHash = transactionsHash;
        this.previousBlockHash = previousBlockHash;
        this.blockHash = blockHash;
        this.minerName = minerName;
        this.minerReward = minerReward;
        this.transactions = transactions;
    }

    public int getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTransactionsHash() {
        return transactionsHash;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public int getNonce() {
        return nonce;
    }

    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public List<SignedTransaction> getTransactions() {
        return transactions;
    }

    public MinerReward getMinerReward() {
        return minerReward;
    }

    public String getMinerName() {
        return minerName;
    }

    @Override
    public String toString() {
        String result = "Block:\n";
        result = result.concat("Created by miner # " + minerName);
        result = result.concat("\n");
        result = result.concat(basicBlockInfo());
        return result;
    }

    private String basicBlockInfo() {
        StringBuilder result = new StringBuilder();
        if (!(minerReward == null)) {
            result.append(minerReward.toString()).append("\n");
        }
        result.append("Id: ").append(id).append("\n");
        result.append("Timestamp: ").append(timestamp).append("\n");
        result.append("Transactions hash:").append("\n");
        result.append(transactionsHash).append("\n");
        result.append("Magic number: ").append(nonce).append("\n");
        result.append("Hash of the previous block: ").append("\n");
        result.append(previousBlockHash).append("\n");
        result.append("Hash of the block: ").append("\n");
        result.append(blockHash).append("\n");
        result.append("Block data: ");
        result.append(blockDataToString());
        return result.toString();
    }

    private String blockDataToString() {
        return transactions.stream()
                .map(SignedTransaction::toString)
                .reduce((sum, next) -> sum += "\n" + next)
                .map(s -> "\n" + s)
                .orElse("no transactions");
    }

    public static Builder newBuilder() {
        return new BlockBuilder();
    }

    public interface Builder {
        Builder setId(int id);
        Builder setTimestamp(long timestamp);
        Builder setTransactionsHash(String transactionsHash);
        Builder setNonce(int nonce);
        Builder setPreviousHash(String hash);
        Builder setHash(String hash);
        Builder setMinerName(String name);
        Builder setTransactions(List<SignedTransaction> transactions);
        Builder setMinerReward(MinerReward reward);
        Block build();
    }

    public static class BlockBuilder implements Builder {
        private int id;
        private long timestamp;
        private String transactionHash;
        private int nonce;
        private String previousHash;
        private String hash;
        private String minerName;
        private List<SignedTransaction> transactions;
        private MinerReward minerReward;

        private BlockBuilder() {
            this.transactionHash = "";
            this.previousHash = "";
            this.hash = "";
            this.minerName = "Unknown";
            transactions = new ArrayList<>();
        }

        @Override
        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @Override
        public Builder setTransactionsHash(String transactionsHash) {
            this.transactionHash = transactionsHash;
            return this;
        }

        @Override
        public Builder setNonce(int nonce) {
            this.nonce = nonce;
            return this;
        }

        @Override
        public Builder setPreviousHash(String hash) {
            this.previousHash = hash;
            return this;
        }

        @Override
        public Builder setHash(String hash) {
            this.hash = hash;
            return this;
        }

        @Override
        public Builder setMinerName(String name) {
            this.minerName = name;
            return this;
        }

        @Override
        public Builder setTransactions(List<SignedTransaction> transactions) {
            this.transactions = transactions;
            return this;
        }

        @Override
        public Builder setMinerReward(MinerReward reward) {
            this.minerReward = reward;
            return this;
        }

        @Override
        public Block build() {
            return new Block(id, timestamp, transactionHash, nonce, previousHash, hash, minerName, minerReward, transactions);
        }
    }
}
