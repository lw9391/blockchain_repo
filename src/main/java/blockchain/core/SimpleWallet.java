package blockchain.core;

import blockchain.encryption.AddressGenerator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Date;

public class SimpleWallet implements Wallet, Serializable {
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final String address;
    private transient Blockchain blockchain;

    private static final long serialVersionUID = 1L;

    public SimpleWallet() {
        KeyPair keyPair = AddressGenerator.CreateKeys();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        address = AddressGenerator.GenerateAddressFromPublicKey(publicKey.getEncoded());
        blockchain = Blockchain.getInstance();
    }

    public SimpleWallet(KeyPair keyPair) {
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        address = AddressGenerator.GenerateAddressFromPublicKey(publicKey.getEncoded());
        blockchain = Blockchain.getInstance();
    }

    @Override
    public SignedTransaction createTransaction(String receiverAddress, long amount) {
        Transaction transaction = new Transaction(address, receiverAddress, amount);
        long timestamp = new Date().getTime();
        String inputForSignature = transaction.toString()
                + "\n" + timestamp;
        byte[] input = inputForSignature.getBytes();
        try {
            byte[] signature = createDigitalSignature(input);
            return new SignedTransaction(transaction, timestamp, signature, publicKey.getEncoded());
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private byte[] createDigitalSignature(byte[] input)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(Blockchain.SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(input);
        return signature.sign();
    }

    @Override
    public boolean sendTransaction(SignedTransaction transaction) {
        return blockchain.addTransaction(transaction);
    }

    @Override
    public byte[] getPublicKey() {
        return publicKey.getEncoded();
    }

    @Override
    public String getAddress() {
        return address;
    }

    public long checkAmountOfCoins() {
        return blockchain.coinsOfClient(address);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.blockchain = Blockchain.getInstance();
    }
}
