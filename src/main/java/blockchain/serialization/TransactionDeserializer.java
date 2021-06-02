package blockchain.serialization;

import blockchain.core.SignedTransaction;
import blockchain.core.Transaction;
import blockchain.encryption.EncryptionUtils;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class TransactionDeserializer implements JsonDeserializer<SignedTransaction> {

    @Override
    public SignedTransaction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String sender = jsonObject.get("sender")
                .getAsString();
        String receiver = jsonObject.get("receiver")
                .getAsString();
        long amount = jsonObject.get("amount")
                .getAsLong();
        Transaction transaction = new Transaction(sender, receiver, amount);
        long timestamp = jsonObject.get("Timestamp")
                .getAsLong();
        byte[] publicKey = EncryptionUtils.decodeHexString(jsonObject.get("PublicKey").getAsString());
        byte[] signature = EncryptionUtils.decodeHexString(jsonObject.get("Signature").getAsString());

        return new SignedTransaction(transaction, timestamp, signature, publicKey);
    }
}
