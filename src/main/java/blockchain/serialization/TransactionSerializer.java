package blockchain.serialization;

import blockchain.core.SignedTransaction;
import blockchain.encryption.EncryptionUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class TransactionSerializer implements JsonSerializer<SignedTransaction> {
    @Override
    public JsonElement serialize(SignedTransaction src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject transactionJson = new JsonObject();
        transactionJson.addProperty("sender", src.getTransaction().getSender());
        transactionJson.addProperty("receiver", src.getTransaction().getReceiver());
        transactionJson.addProperty("amount", src.getTransaction().getAmount());
        transactionJson.addProperty("Timestamp", src.getTimestamp());

        String publicKey = EncryptionUtils.encodeIntoHex(src.getPublicKey());
        String signature = EncryptionUtils.encodeIntoHex(src.getSignature());

        transactionJson.addProperty("Signature", signature);
        transactionJson.addProperty("PublicKey", publicKey);

        return transactionJson;
    }
}
