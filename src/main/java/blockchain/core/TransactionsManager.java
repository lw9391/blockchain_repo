package blockchain.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public final class TransactionsManager implements Serializable {
    private final List<SignedTransaction> pendingTransactions;
    private transient TransactionValidator validator;
    private transient Blockchain blockchain;
    private static final long serialVersionUID = 1L;
    public static transient Logger LOGGER = LoggerFactory.getLogger(TransactionsManager.class);

    public TransactionsManager(Blockchain blockchain) {
        this.pendingTransactions = new ArrayList<>();
        this.blockchain = blockchain;
        this.validator = new TransactionValidator();
    }

    public TransactionsManager(Blockchain blockchain, List<SignedTransaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
        this.blockchain = blockchain;
        this.validator = new TransactionValidator();
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

    public boolean checkTransactionValidity(SignedTransaction signedTransaction, List<Block> blocks, List<SignedTransaction> pendingTransactionsList) {
        boolean timeValidity = validator.checkTransactionTimeValidity(signedTransaction, blocks, pendingTransactionsList);

        boolean balanceValidity = validator.checkBalanceValidity(signedTransaction, blocks, pendingTransactionsList);

        boolean signatureValidity = validator.checkSignatureValidity(signedTransaction);

        return timeValidity && balanceValidity && signatureValidity;
    }

    public List<SignedTransaction> getPendingTransactions() {
        return new ArrayList<>(pendingTransactions);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.blockchain = Blockchain.getInstance();
        this.validator = new TransactionValidator();
        LOGGER = LoggerFactory.getLogger(TransactionsManager.class);
    }

    public long coinsOfClient(String client) {
        return validator.coinsOfClient(client, blockchain.getBlockList(), pendingTransactions);
    }
}
