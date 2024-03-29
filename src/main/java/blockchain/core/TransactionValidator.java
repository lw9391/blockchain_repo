package blockchain.core;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public boolean checkTransactionsSignatures(Block block) {
        List<SignedTransaction> transactionsToCheck = block.getTransactions();
        return checkSignaturesOfTransactionsInList(transactionsToCheck);
    }

    private boolean checkSignaturesOfTransactionsInList(List<SignedTransaction> transactions) {
        long count = transactions.stream()
                .filter(this::checkSignatureValidity)
                .count();

        return count == transactions.size();
    }

    public boolean checkNewBlockOutgoings(Block newBlock, Map<String, Long> balanceMap) {
        Map<String, Long> outgoingsOfNewBlock = getMapOfOutgoings(newBlock);
        long count = outgoingsOfNewBlock.keySet()
                .stream()
                .filter(address -> {
                    Long outgoing = outgoingsOfNewBlock.get(address);
                    Long balance = balanceMap.get(address);
                    return balance == null || balance < -1 * outgoing;
                })
                .count();
        return count == 0;
    }

    /* Returns map containing only spent coins of each address */
    public Map<String, Long> getMapOfOutgoings(Block block) {
        Map<String, Long> outgoings = new HashMap<>();
        block.getTransactions().forEach(updateSenderBalances(outgoings));
        return outgoings;
    }

    public boolean checkBalanceMap(Map<String, Long> balanceMap) {
        assert balanceMap.size() > 0;
        double lowestBalance = balanceMap.values()
                .stream()
                .mapToLong(Long::longValue)
                .min()
                .getAsLong();
        return lowestBalance >= 0;
    }

    /* In a given part (or whole) blockchain checks for duplicated transactions. Transaction is considered to be duplicated if the
     * same sender has two or more transactions with the same time. */
    public boolean hasDuplicatedTransactions(List<Block> blocks) {
        long duplications = blocks.stream()
                .flatMap(block -> block.getTransactions().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(m -> m.getValue() > 1)
                .count();
        return (duplications > 0);
    }

    public boolean hasDuplicatedTransactions(Block block, List<Block> existingBlocks) {
        long duplications = Stream.concat(existingBlocks.stream().flatMap(block1 -> block1.getTransactions().stream()), block.getTransactions().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(m -> m.getValue() > 1)
                .count();
        return (duplications > 0);
    }

    /* This method update map with clients balances based on provided block. Requires balance map prepared of previous block or empty map
     * if this is first block in blockchain. Recommended to use hash map for efficiency purposes. */
    public Map<String, Long> updateBalanceMap(Block block, Map<String, Long> balanceMap) {
        block.getTransactions().forEach(updateBalanceMap(balanceMap));
        return balanceMap;
    }

    public Map<String, Long> updateBalanceMap(MinerReward reward, Map<String, Long> balanceMap) {
        String miner = reward.getMiner();
        long rewardValue = reward.getReward();
        if (balanceMap.containsKey(miner)) {
            long oldBalance = balanceMap.get(miner);
            balanceMap.replace(miner, oldBalance + rewardValue);
        } else {
            balanceMap.put(miner, rewardValue);
        }
        return balanceMap;
    }

    private Consumer<SignedTransaction> updateBalanceMap(Map<String, Long> mapToUpdate) {
        Consumer<SignedTransaction> updateSendersBalances = updateSenderBalances(mapToUpdate);
        Consumer<SignedTransaction> updateReceiversBalances = (signedTransaction) -> {
            String receiver = signedTransaction.getTransaction().getReceiver();
            long amount = signedTransaction.getTransaction().getAmount();
            if (mapToUpdate.containsKey(receiver)) {
                long prevBalance = mapToUpdate.get(receiver);
                long balance = prevBalance + amount;
                mapToUpdate.replace(receiver, balance);
            } else {
                mapToUpdate.put(receiver, amount);
            }
        };
        return updateReceiversBalances.andThen(updateSendersBalances);
    }

    private Consumer<SignedTransaction> updateSenderBalances(Map<String, Long> mapToUpdate) {
        return  (signedTransaction) -> {
            String sender = signedTransaction.getTransaction().getSender();
            long amount = signedTransaction.getTransaction().getAmount();
            if (mapToUpdate.containsKey(sender)) {
                long prevBalance = mapToUpdate.get(sender);
                long balance = prevBalance - amount;
                mapToUpdate.replace(sender, balance);
            } else {
                mapToUpdate.put(sender, -amount);
            }
        };
    }
}
