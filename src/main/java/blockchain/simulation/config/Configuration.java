package blockchain.simulation.config;

public class Configuration {
    /* Base configuration, used if no specific config file is provided or file is missing some particular information. */
    private int numberOfMiners = 4;
    private int numberOfClients = 4;
    private int assumedBlockchainSize = 10;
    private int initialClientsDelay = 100; //milliseconds
    private int clientsTransactionsDelay = 500; //milliseconds
    private boolean useFullTransactionsCheckOnLoading = false;

    public Configuration() {
        super();
    }

    public int getNumberOfMiners() {
        return numberOfMiners;
    }

    public int getNumberOfClients() {
        return numberOfClients;
    }

    public int getAssumedBlockchainSize() {
        return assumedBlockchainSize;
    }

    public int getInitialClientsDelay() {
        return initialClientsDelay;
    }

    public int getClientsTransactionsDelay() {
        return clientsTransactionsDelay;
    }

    public void setNumberOfMiners(int numberOfMiners) {
        this.numberOfMiners = numberOfMiners;
    }

    public void setNumberOfClients(int numberOfClients) {
        this.numberOfClients = numberOfClients;
    }

    public void setAssumedBlockchainSize(int assumedBlockchainSize) {
        this.assumedBlockchainSize = assumedBlockchainSize;
    }

    public void setInitialClientsDelay(int initialClientsDelay) {
        this.initialClientsDelay = initialClientsDelay;
    }

    public void setClientsTransactionsDelay(int clientsTransactionsDelay) {
        this.clientsTransactionsDelay = clientsTransactionsDelay;
    }

    public boolean isUseFullTransactionsCheckOnLoading() {
        return useFullTransactionsCheckOnLoading;
    }

    public void setUseFullTransactionsCheckOnLoading(boolean useFullTransactionsCheckOnLoading) {
        this.useFullTransactionsCheckOnLoading = useFullTransactionsCheckOnLoading;
    }
}
