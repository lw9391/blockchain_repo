# Blockchain Simulator
Simple blockchain implementation made from scratch.

## General information
This is simple cryptocurrency application. At this stage of project, network part is still under development so 
whole application work as simulator. After running, it will spawn configured number of miner and 
client threads and generate example of blockchain according to protocol it is following. For now, you can treat 
application as demo version.

For more detail information of how this particular cryptocurrency is designed read _protocol.md_

## Technologies
* Java 16
* Gradle 7.0.2
* Gson 2.8.6
* Slf4j 1.7.30 and Logback 1.2.3

## Launch
Navigate to project folder and build it with gradle wrapper using `./gradlew build` command. After that you can unpack
zip file from _build/distribution_ folder, navigate to _bin_ and start it by typing `blockchain` in console. This will 
start simulation with basic configuration.

### Configuration
You can configure some of the application's settings by providing _config.json_ file. Create config folder in _bin_ 
directory (or in project directory if you want to run it from IDE). 
List of parameters you can configure with its names in _config.json_ file:
1. Number of miner: "numberOfMiners"
2. Number of clients: "numberOfClients"
3. Blockchain size to generate: "assumedBlockchainSize"
4. Initial delay time after which client thread pool will start sending transactions: "initialClientsDelay"
5. Delay between each subsequent transaction sent: "clientsTransactionsDelay"
In case no config file is included or config file includes only part of the data application uses its basic configuration.
Snippet below shows example of config.json file, with all fields set to the same values as basic configuration:

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
After finishing simulation, generated blockchain along with miners, clients and pending transactions will be stored 
in serialization_output folder as standard java serialization output. Along with mentioned files you can find 
blockchain.json file which contains blockchain in human-readable format.

### Running again
If you want to continue with already created blockchain, simply increase assumed size in _config.json_ file and run 
application. In case you want to start a new one just delete serialization output files.

### Changing config file
When continuing created blockchain, changing number of miner and clients won't apply to your simulation unless you 
delete them (_miners_ and _clients_ files). This will force application to create new miners and clients in amount 
taken from config file. Just remember that coins associated to their addresses in blockchain will be lost.

## Project status
In development. 

## Sources
Application is inspired by Bitcoin and other cryptocurrency projects.