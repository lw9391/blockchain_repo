package blockchain.serialization;

import blockchain.core.Block;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class BlockSerializer implements JsonSerializer<Block> {

    @Override
    public JsonElement serialize(Block src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonBlock = new JsonObject();
        jsonBlock.addProperty("Id", src.getId());
        jsonBlock.addProperty("Timestamp", src.getTimestamp());
        jsonBlock.addProperty("TransactionsHash", src.getTransactionsHash());
        jsonBlock.addProperty("nonce", src.getNonce());
        jsonBlock.addProperty("prevHash", src.getPreviousBlockHash());
        jsonBlock.addProperty("blockHash", src.getBlockHash());
        jsonBlock.addProperty("minerName", src.getMinerName());

        JsonElement rewardElement = context.serialize(src.getMinerReward());
        JsonElement transactionElement = context.serialize(src.getTransactions());

        jsonBlock.add("MinerReward", rewardElement);
        jsonBlock.add("Transactions", transactionElement);

        return jsonBlock;
    }
}
