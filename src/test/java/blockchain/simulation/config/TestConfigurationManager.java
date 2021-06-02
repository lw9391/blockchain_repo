package blockchain.simulation.config;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;

public class TestConfigurationManager {
    private static ConfigurationManager cm;

    @BeforeAll
    static void setUp() {
        cm = ConfigurationManager.getInstance();
    }

    @Test
    public void testLoadConfiguration() throws FileNotFoundException {
        cm.loadConfiguration("src/test/resources/config.json");
        Configuration configuration = cm.getCurrentConfiguration();
        assertEquals(configuration.getNumberOfMiners(), 4);
        assertEquals(configuration.getNumberOfClients(), 4);
        assertEquals(configuration.getAssumedBlockchainSize(), 10);
        assertEquals(configuration.getInitialClientsDelay(), 150);
        assertEquals(configuration.getClientsTransactionsDelay(), 500);
    }
}
