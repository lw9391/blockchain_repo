package blockchain.core;

import blockchain.core.SignedTransaction;

public interface Wallet {
    SignedTransaction createTransaction(String address, long amount);
    boolean sendTransaction(SignedTransaction transaction);
    byte[] getPublicKey();
    String getAddress();
    long checkAmountOfCoins();
}
