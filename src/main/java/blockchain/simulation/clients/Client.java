package blockchain.simulation.clients;

import blockchain.core.SimpleWallet;
import blockchain.core.Wallet;
import blockchain.core.SignedTransaction;

import java.io.Serializable;
import java.util.Objects;

public class Client implements Serializable {
    private final Wallet wallet;
    private final String name;

    public Client(String name) {
        this.name = name;
        this.wallet = new SimpleWallet();
    }

    public void sendTransactionToBlockchain(String receiver, long amount) {
        SignedTransaction transaction = wallet.createTransaction(receiver, amount);
        wallet.sendTransaction(transaction);
    }

    private double updateAmountOfCoins() {
        return wallet.checkAmountOfCoins();
    }

    public byte[] getPublicKey() {
        return wallet.getPublicKey();
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return wallet.getAddress();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return Objects.equals(wallet, client.wallet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, wallet);
    }

}