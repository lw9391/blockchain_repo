# Blockchain Simulator
Simple blockchain implementation made from scratch.

## General information
This is a simple cryptocurrency application. At this stage of project, a network part is still under development, so the
whole application work as simulator. After running, it will spawn configured number of miner and 
client threads and generate example of blockchain according to the protocol it is following. For now, you can treat the 
application as demo version.

For more detail information of how this particular cryptocurrency is designed read _protocol.md_

## Technologies
* Java 16
* Gradle 7.0.2
* Gson 2.8.6
* Slf4j 1.7.30 and Logback 1.2.3

## Launch
Navigate to the project folder and build it with a gradle wrapper using `./gradlew build` command. After that you can unpack
a zip file from _build/distribution_ folder, navigate to the _bin_ and start it by typing `blockchain` in the console. 
This will start a simulation with basic configuration.

### Configuration
You can configure some of the application's settings by providing a _config.json_ file. Create a config folder in the _bin_ 
directory (or in the project directory if you want to run it from an IDE). 
List of parameters you can configure with its names in a _config.json_ file:
1. Number of miner: "numberOfMiners"
2. Number of clients: "numberOfClients"
3. Blockchain size to generate: "assumedBlockchainSize"
4. Initial delay time after which a client thread pool will start sending transactions: "initialClientsDelay"
5. Delay between each subsequent transaction sent: "clientsTransactionsDelay"
In case no config file is included, or a config file includes only part of the data, the application uses its basic configuration.
Snippet below shows example of a config.json file, with all fields set to the same values as basic configuration:

~~~
{
  "numberOfMiners": 4,
  "numberOfClients": 4,
  "assumedBlockchainSize": 10,
  "initialClientsDelay": 100,
  "clientsTransactionsDelay": 500
}
~~~
### Output
After finishing a simulation, generated blockchain along with miners, clients and pending transactions will be stored 
in a serialization_output folder as a standard java serialization output. Along with mentioned files you can find 
a blockchain.json file which contains blockchain in human-readable format.

### Running again
If you want to continue with already created blockchain, simply increase an assumed size in the _config.json_ file and 
run the application. In case you want to start a new one just delete serialization output files.

### Changing config file
When continuing a created blockchain, changing number of miners and clients won't apply to your simulation unless you 
delete them (_miners_ and _clients_ files). This will force the application to create new miners and clients in amount 
taken from the config file. Just remember that coins associated to their addresses in blockchain will be lost.

## Project status
In development. 

## Sources
Application is inspired by Bitcoin and other cryptocurrency projects.