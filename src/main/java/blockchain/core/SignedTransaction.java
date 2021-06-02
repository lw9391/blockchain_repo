package blockchain.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class SignedTransaction implements Serializable {
    private final Transaction transaction;
    private final long timestamp;
    private final byte[] signature;
    private final byte[] publicKey;

    public SignedTransaction(Transaction transaction, long timestamp, byte[] signature, byte[] publicKey) {
        this.transaction = transaction;
        this.timestamp = timestamp;
        this.signature = signature;
        this.publicKey = publicKey;
    }

    public byte[] getSignature() {
        return signature;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    @Override
    public String toString() {
        return transaction.toString();
    }

    public String stringForHashing() {
        return toString() + "\n" + timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignedTransaction that = (SignedTransaction) o;
        return timestamp == that.timestamp &&
                Objects.equals(transaction, that.transaction) &&
                Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(transaction, timestamp);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }
}
