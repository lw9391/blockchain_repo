package blockchain.simulation.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class ConfigurationManager {
    private static ConfigurationManager configurationManager;
    private static Configuration currentConfiguration;

    private ConfigurationManager() {
        super();
    }

    public static ConfigurationManager getInstance() {
        if (configurationManager == null) {
            configurationManager = new ConfigurationManager();
        }
        return configurationManager;
    }
    public void loadConfiguration(String filepath) throws FileNotFoundException {
        FileReader fileReader = new FileReader(filepath);
        Gson gson = new GsonBuilder().create();
        currentConfiguration = gson.fromJson(fileReader, Configuration.class);
    }

    public Configuration getCurrentConfiguration() {
        return currentConfiguration;
    }

    public Configuration getBaseConfiguration() {
        return new Configuration();
    }
}
