package blockchain.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class TransactionsManager implements Serializable {
    private final List<SignedTransaction> pendingTransactions;
    private transient Blockchain blockchain;
    private static final long serialVersionUID = 1L;
    public static transient Logger LOGGER = LoggerFactory.getLogger(TransactionsManager.class);

    public TransactionsManager(Blockchain blockchain) {
        this.pendingTransactions = new ArrayList<>();
        this.blockchain = blockchain;
    }

    public TransactionsManager(Blockchain blockchain, List<SignedTransaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
        this.blockchain = blockchain;
    }

    public boolean checkNewBlockTransactions(Block nextBlock) {
        List<SignedTransaction> transactions = nextBlock.getTransactions();
        return pendingTransactions.containsAll(transactions);
    }

    public void removeTransactionsAddedInNewBlock(Block newBlock) {
        List<SignedTransaction> transactions = newBlock.getTransactions();
        pendingTransactions.removeAll(transactions);
    }

    public boolean addTransaction(SignedTransaction transaction) {
        if (checkTransactionValidity(transaction, blockchain.getBlockList(), pendingTransactions)) {
            return pendingTransactions.add(transaction);
        }
        return false;
    }

    private boolean checkTransactionValidity(SignedTransaction signedTransaction, List<Block> blocks, List<SignedTransaction> pendingTransactionsList) {
        boolean timeValidity = checkTransactionTimeValidity(signedTransaction, blocks, pendingTransactionsList);

        boolean balanceValidity = checkBalanceValidity(signedTransaction, blocks, pendingTransactionsList);

        boolean signatureValidity = checkSignatureValidity(signedTransaction);

        return timeValidity && balanceValidity && signatureValidity;
    }

    /* For security purposes, date of new client transaction must be later than his last transaction. This prevent from
     * copying the same transaction over again by unauthorized users */
    private boolean checkTransactionTimeValidity(SignedTransaction signedTransaction, List<Block> blocks, List<SignedTransaction> pendingTransactionsList) {
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

    private boolean checkBalanceValidity(SignedTransaction signedTransaction, List<Block> blocks, List<SignedTransaction> pendingTransactionsList) {
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

    private boolean checkSignatureValidity(SignedTransaction signedTransaction) {
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

    public long coinsOfClient(String client) {
        return coinsOfClient(client, blockchain.getBlockList(), pendingTransactions);
    }

    private long coinsOfClient(String client, List<Block> blocks, List<SignedTransaction> pendingTransactionsList) {
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

    public List<SignedTransaction> getPendingTransactions() {
        return new ArrayList<>(pendingTransactions);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.blockchain = Blockchain.getInstance();
        LOGGER = LoggerFactory.getLogger(TransactionsManager.class);
    }
}
