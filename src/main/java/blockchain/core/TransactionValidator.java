package blockchain.core;

import blockchain.simulation.clients.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TransactionValidator {
    private static Logger LOGGER = LoggerFactory.getLogger(TransactionValidator.class);

    /* For security purposes, date of new client transaction must be later than his last transaction. This prevent from
     * copying the same transaction over again by unauthorized users */
    public boolean checkTransactionTimeValidity(SignedTransaction signedTransaction, List<Block> blocks, List<SignedTransaction> pendingTransactionsList) {
        String sender = signedTransaction.getTransaction()
                .getSender();
        long timestamp = signedTransaction.getTimestamp();
        Long timeOfClientLastTransaction = matchLatestTransactionOfClient(sender, blocks, pendingTransactionsList)
                .map(SignedTransaction::getTimestamp)
                .orElse(0L);
        if (timestamp < timeOfClientLastTransaction) {
            LOGGER.warn("Wrong time");
            return false;
        }
        return true;
    }

    private Optional<SignedTransaction> matchLatestTransactionOfClient(String searchedClient, List<Block> blocks, List<SignedTransaction> pendingTransactionsList) {
        return checkPendingTransactions(searchedClient, pendingTransactionsList)
                .or(checkBlockchainForTransactions(searchedClient, blocks));
    }

    private Optional<SignedTransaction> checkPendingTransactions(String searchedClient, List<SignedTransaction> pendingTransactionsList) {
        return findLastClientTransactionFromList(pendingTransactionsList, searchedClient);
    }

    private Supplier<Optional<SignedTransaction>> checkBlockchainForTransactions(String searchedClient, List<Block> blocks) {
        return () -> blocks.stream()
                .skip(1)
                .flatMap(block -> block.getTransactions().stream())
                .filter(filterBySender(searchedClient))
                .reduce((first, second) -> second);
    }

    private Optional<SignedTransaction> findLastClientTransactionFromList(List<SignedTransaction> list, String client) {
        return list.stream()
                .filter(filterBySender(client))
                .reduce((first, second) -> second);
    }

    private Predicate<SignedTransaction> filterBySender(String sender) {
        return transaction -> transaction.getTransaction()
                .getSender()
                .equals(sender);
    }

    private Predicate<SignedTransaction> filterByReceiver(String receiver) {
        return transaction -> transaction.getTransaction()
                .getReceiver()
                .equals(receiver);
    }

    public long coinsOfClient(String client, List<Block> blocks, List<SignedTransaction> pendingTransactionsList) {
        long spend = coinsSpentByClient(client, blocks, pendingTransactionsList);
        long received = coinsReceivedByClient(client, blocks);
        long mined = coinsMined(client, blocks);
        long balance = received + mined - spend;
        if (balance < 0) {
            throw new RuntimeException("Negative balance occurred, shutting down simulation.");
        }
        return balance;
    }

    private long coinsSpentByClient(String client, List<Block> blocks, List<SignedTransaction> pendingTransactionsList) {
        long coinsFromBlocks = coinsSpentByClientInBlocks(client, blocks);
        long coinsFromTransactionToPublish = coinsSpentByClientInPendingList(client, pendingTransactionsList);
        return coinsFromBlocks + coinsFromTransactionToPublish;
    }

    private long coinsSpentByClientInBlocks(String client, List<Block> blocks) {
        return blocks.stream()
                .skip(1)
                .flatMap(block -> block.getTransactions().stream())
                .filter(filterBySender(client))
                .mapToLong(signedTransaction -> signedTransaction.getTransaction().getAmount())
                .reduce((sum, next) -> sum += next)
                .orElse(0);
    }

    private long coinsSpentByClientInPendingList(String client, List<SignedTransaction> pendingTransactionsList) {
        return pendingTransactionsList.stream()
                .filter(filterBySender(client))
                .mapToLong(signedTransaction -> signedTransaction.getTransaction().getAmount())
                .reduce((sum, next) -> sum += next)
                .orElse(0);
    }

    private long coinsReceivedByClient(String client, List<Block> blocks) {
        return blocks.stream()
                .skip(1)
                .flatMap(block -> block.getTransactions().stream())
                .filter(filterByReceiver(client))
                .mapToLong(signedTransaction -> signedTransaction.getTransaction().getAmount())
                .reduce((sum, next) -> sum += next)
                .orElse(0);
    }

    private long coinsMined(String miner, List<Block> blocks) {
        return blocks.stream()
                .skip(1)
                .map(Block::getMinerReward)
                .filter(reward -> reward.getMiner().equals(miner))
                .mapToLong(MinerReward::getReward)
                .reduce((sum, next) -> sum += next)
                .orElse(0);
    }

    public boolean checkBalanceValidity(SignedTransaction signedTransaction, List<Block> blocks, List<SignedTransaction> pendingTransactionsList) {
        double amount = signedTransaction.getTransaction()
                .getAmount();
        if (amount <= 0) {
            LOGGER.warn("Transaction value wrong: " + amount);
            return false;
        }
        String sender = signedTransaction.getTransaction().getSender();
        double balance = coinsOfClient(sender, blocks, pendingTransactionsList);
        if (amount > balance) {
            LOGGER.warn("Bad balance");
            return false;
        }
        return true;
    }

    public boolean checkSignatureValidity(SignedTransaction signedTransaction) {
        String inputForSignature = signedTransaction.toString() + "\n" + signedTransaction.getTimestamp();
        byte[] input = inputForSignature.getBytes();
        byte[] signature = signedTransaction.getSignature();
        boolean signatureVerification;
        try {
            signatureVerification = verifySignature(input, signature, signedTransaction.getPublicKey());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Wrong algorithm exception occurred", e);
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            LOGGER.error("Wrong key exception", e);
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            LOGGER.error("Signature exception", e);
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("KeySpec exception", e);
            throw new RuntimeException(e);
        }
        if (!signatureVerification) {
            LOGGER.error("Wrong signature.");
            return false;
        }
        return true;
    }

    private boolean verifySignature(byte[] input, byte[] signatureToVerify, byte[] keyBytes)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidKeySpecException {
        Signature signature = Signature.getInstance(Blockchain.SIGNATURE_ALGORITHM);
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);
        signature.initVerify(publicKey);
        signature.update(input);
        return signature.verify(signatureToVerify);
    }


    /*  -----------------------  */
    /* Faster way for checking block transactions, this method only checks signatures. Should be enough for most cases. */
    public boolean checkExistingBlockTransactions(Block block) {
        List<SignedTransaction> transactionsToCheck = block.getTransactions();
        return checkSignaturesOfTransactionsInList(transactionsToCheck);
    }

    /* Unlike the method above, this also checks clients balances. Requires balance map prepared of previous blocks. */
    public boolean checkExistingBlockTransactions(Block block, Map<Client, Double> balanceMap) {
        List<SignedTransaction> transactionsList = block.getTransactions();

        boolean signaturesCheck = checkSignaturesOfTransactionsInList(transactionsList);

        boolean balanceCheck = checkBalanceMap(balanceMap);

        return signaturesCheck && balanceCheck;
    }

    /* In a given part (or whole) blockchain checks for duplicated transactions. Transaction is considered to be duplicated if the
     * same sender has two or more transactions with the same time. */
    public boolean checkForDuplicatedTransactions(List<Block> blocks) {
        long duplications = blocks.stream()
                .flatMap(block -> block.getTransactions().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(m -> m.getValue() > 1)
                .count();
        return !(duplications > 0);
    }

    private boolean checkSignaturesOfTransactionsInList(List<SignedTransaction> transactions) {
        long count = transactions.stream()
                .filter(this::checkSignatureValidity)
                .count();

        return count == transactions.size();
    }

    /* This method update map with clients balances based on provided block. Requires balance map prepared of previous block or empty map
     * if this is first block in blockchain. Recommended to use hash map for efficiency purposes. */
    public Map<String, Double> updateBalanceMap(List<SignedTransaction> transactionList, Map<String, Double> balanceMap) {
        transactionList.forEach(updateBalanceMap(balanceMap));
        return balanceMap;
    }

    public Map<String, Double> updateBalanceMap(MinerReward reward, Map<String, Double> balanceMap) {
        String miner = reward.getMiner();
        double rewardValue = reward.getReward();
        if (balanceMap.containsKey(miner)) {
            double oldBalance = balanceMap.get(miner);
            balanceMap.replace(miner,oldBalance + rewardValue);
        } else {
            balanceMap.put(miner, rewardValue);
        }
        return balanceMap;
    }

    private Consumer<SignedTransaction> updateBalanceMap(Map<String, Double> mapToUpdate) {
        Consumer<SignedTransaction> updateSendersBalances = (signedTransaction) -> {
            String sender = signedTransaction.getTransaction().getSender();
            double amount = signedTransaction.getTransaction().getAmount();
            if (mapToUpdate.containsKey(sender)) {
                double prevBalance = mapToUpdate.get(sender);
                double balance = prevBalance - amount;
                mapToUpdate.replace(sender, balance);
            } else {
                mapToUpdate.put(sender, -amount);
            }
        };
        Consumer<SignedTransaction> updateReceiversBalances = (signedTransaction) -> {
            String receiver = signedTransaction.getTransaction().getReceiver();
            double amount = signedTransaction.getTransaction().getAmount();
            if (mapToUpdate.containsKey(receiver)) {
                double prevBalance = mapToUpdate.get(receiver);
                double balance = prevBalance + amount;
                mapToUpdate.replace(receiver, balance);
            } else {
                mapToUpdate.put(receiver, amount);
            }
        };
        return updateReceiversBalances.andThen(updateSendersBalances);
    }

    private boolean checkBalanceMap(Map<Client, Double> balanceMap) {
        assert balanceMap.size() > 0;
        double lowestBalance = balanceMap.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .getAsDouble();
        return lowestBalance >= 0;
    }
}
