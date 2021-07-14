# Blockchain protocol
Document describes simple protocol this application is following.
Applies to transactions, blocks, validation and hashing as network part of the application
is still under development.

# Basic information:
* Consensus mechanism: proof-of-work, known as mining
* Key algorithm: RSA
* Keys length: 1024 bits
* Signature algorithm: Sha256 with RSA

## Block's structure
Each block consists of following elements:
* Id
* Timestamp (creation time)
* Transactions hash
* Nonce
* Hash of the previous block
* Hash of this block
* Miner name - the only optional field, not included in input for hashing
* Miner reward
* Block data, in this case list of transactions

The very first block in blockchain has id equals 0, hash equals zero, rest of its data is empty. It can be considered 
as genesis block, its main purpose is to provide hash for next block.

Snippet below shows example of valid block (in JSON format) with one transaction, key and signature 
truncated for readability.
~~~
{
    "Id": 2,
    "Timestamp": 1624901479699,
    "TransactionsHash": "2bcbca0b6e6c92461ff0a12fe3fc519d2eb481a48c6a52fce6f9c720010475cf",
    "nonce": 463288268,
    "prevHash": "0000019322e7548388abc623fc21823e05458760c95881e6bff7375697843df9",
    "blockHash": "000000a94cf1c8a83dccd8c414826fa690ea436ec0ae3a5acbc3d26c82b31b64",
    "minerName": "1",
    "MinerReward": {
      "miner": "19o1nYPBbEg3YCLVVKz5xiJ4vWDKopCSxa",
      "reward": 100
    },
    "Transactions": [
      {
        "sender": "19o1nYPBbEg3YCLVVKz5xiJ4vWDKopCSxa",
        "receiver": "157mSkBby2tXzSjxUx8PzMVQXJvzZ4rpqH",
        "amount": 28,
        "Timestamp": 1624901479687,
        "Signature": "d3498f....",
        "PublicKey": "30819f...."
      }
    ]
  }
~~~

## Transaction's structure
* Sender address
* Amount of coins
* Receiver address
* Timestamp
* Public key of sender
* Signature

## Miner reward
Miner reward is special kind of transaction, a reward miner receives for generating new block. String example below:
~~~
19o1nYPBbEg3YCLVVKz5xiJ4vWDKopCSxa gets 100 VC
~~~
## Mining difficulty and nonce AKA magic number
Application requires each block hash to start with particular number of zeros. One can find required hash by 
changing nonce until hashed block has required number of zeros. Difficulty increases if miners find new blocks faster.
### Mining difficulty
Every 3 appended blocks, application checks average creation time of them. If creation time differs from the target 
time by more than the assumed tolerance, application will change difficulty.

## Hashing
For blocks and transactions, single sha-256 hash is used.

### Blocks hashing
Application hashes blocks according to the pattern below:
~~~
<Miner_rewar><id><timestamp><nonce><prevoius_hash><transactions_hash>
~~~

### Transactions hashing
Application uses first four fields to hash transactions lists, according to template shown below:
~~~
<sender1> sent <amount1> VC to <receiver1>
<timestamp1>
<sender2> sent <amount2> VC to <receiver2>
<timestamp2>
...
<senderN> sent <amountN> VC to <receiverN>
<timestampN>
~~~
In case block contains no transactions, applications hashes "No transactions" string.
## Addresses
Basically, application uses the same address generation mechanism as bitcoin has.
### Creation
Client address is the hash of its public key, more accurately key is hashed twice, first time with sha-256 and then with
RIPEMD160. Generating address comes with accordance to following points:
1. Hash public key twice, one with sha256 and then with RIPEMD160.
2. Append version number at the beginning of the hashed key.
3. Take hashed key with version number and generate checksum for it.
4. Append checksum at the end of key with version number.
Addresses are displayed in Base58 encoding.
### Base58
Base58 was created specially for bitcoins addresses encoding. It's a human-readable and gets rid of characters that looks
similar (0,O,l,I) to reduce number of typos. 
### Version prefix
00 for that version of application. This makes every address encoded in Base58 to start with '1' digit.
### Checksum
Checksum is first four bytes of double sha256 hash of whatever is checked. The use of a checksum allows to detect an 
incorrectly entered address and can prevent assign coins to non-existing addresses.
## Balance
After broadcasting new transaction, application stores it in pending list. When checking balances, this counts as 
spent coins for sender, however, receiver balance don't include that coins. 
This rule should prevent double-spending coins.

## Validation

### Transaction validation
There are three requirements for a transaction to be a valid one:

#### Balance
Sender balance need to be higher than transaction amount. As mentioned in _Balance_ chapter, unpublished transactions
waiting in pending lists are taken into account as spent coins for senders.
#### Signature
Transaction requires signature to prove that it was sent by actual owner of that coins. Input for signature is almost 
the same as input for transactions list hashing, with one exception that we are using one transaction not
whole list.
~~~
<sender> sent <amount> VC to <receiver>
<timestamp>
~~~
#### Timestamp
Transaction creation time need to be later than previous transaction creation time from the same sender - this mainly
serves as protection form copying and broadcasting already published transaction. System will reject copied transactions.
Without this parameter, there would be possibility to broadcast the same transactions over and over again because the
signature would be always the same.

### Block validation
#### Transactions
All transactions in attached list must be valid and currently waiting in pending list. Transactions hash must be done 
correctly according to template described in _Hashing_ chapter.
#### Id
Block must have correct id, no missing ids are allowed.
#### Timestamp
Block creation timestamp must be lower than current timestamp. This isn't particular good verification, and it will
be changed in the future versions.
#### Previous block hash
Like mentioned in chapter *Block's structure*, block contains hash of previous block as one of its fields.
#### Block hash
Application checks if hash starts with required number of zeros, based on current difficulty value and checks if hash
was prepared correctly.