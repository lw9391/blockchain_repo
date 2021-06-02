package blockchain.core;

import java.io.Serializable;
import java.util.Objects;

public class Transaction implements Serializable {
    private final String sender;
    private final String receiver;
    private final long amount;

    public Transaction(String sender, String receiver, long amount) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return sender + " sent " + amount + " VC to " + receiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return amount == that.amount &&
                Objects.equals(sender, that.sender) &&
                Objects.equals(receiver, that.receiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, receiver, amount);
    }
}
