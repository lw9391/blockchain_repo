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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TransactionValidator {
    private static Logger LOGGER = LoggerFactory.getLogger(TransactionValidator.class);




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
}
