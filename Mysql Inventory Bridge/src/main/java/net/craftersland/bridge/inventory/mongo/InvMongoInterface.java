package net.craftersland.bridge.inventory.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.encoder.EncodeResult;
import net.craftersland.bridge.inventory.encoder.Encoder;
import net.craftersland.bridge.inventory.encoder.EncoderFactory;
import net.craftersland.bridge.inventory.objects.PlayerDataType;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;

public class InvMongoInterface {

    @Getter private static InvMongoInterface instance;
    private final Main main;
    private MongoClient mongoConnection;

    private InvMongoInterface(Main main) {
        this.main = main;
        try {
            this.mongoConnection = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
        }catch (MongoException exc) {
            Bukkit.getLogger().log(Level.SEVERE, main.getName() + ": Error connecting to mongoDB.");
            this.mongoConnection = null;
        }

    }

    public static void createInstance(Main main) {
        if (instance == null) instance = new InvMongoInterface(main);
    }

    public void setData(Collection<Object[]> data) {

        Main.log.info("DEBUG 0");

        for (Object[] player : data) {

            Main.log.info("DEBUG 1");

            if(player == null || player.length == 0)
                continue;

            boolean result = updatePlayerData((UUID) player[0], (EncodeResult) player[1],
                    (EncodeResult) player[2]);

            if (!result)
                Bukkit.getLogger().log(Level.SEVERE, ": Error uploading data from player " + player[0].toString() + " to MongoDB.");

        }

    }

    public boolean updatePlayerData(UUID playerUUID, EncodeResult inventory, EncodeResult armor) {

        if (this.mongoConnection == null) return false;

        MongoDatabase mongoDatabase = mongoConnection.getDatabase("minecraft_inventory_bridge");
        MongoCollection<Document> transactionsCollection = mongoDatabase.getCollection("transactions");

        transactionsCollection.insertOne(
            new Document("playerID", playerUUID.toString())
                    .append("timestamp", new Timestamp(System.currentTimeMillis()))
                    .append("data", new Document("inventory", inventory).append("armor", armor))
        );

        return true;

    }

    public List<EncodeResult> getPlayerData(UUID playerUUID, PlayerDataType playerDataType) {

        MongoDatabase mongoDatabase = mongoConnection.getDatabase("minecraft_inventory_bridge");
        MongoCollection<Document> transactionsCollection = mongoDatabase.getCollection("transactions");
        List<EncodeResult> playerEncodedData = new ArrayList<>();

        for (Document nextDocument : transactionsCollection.find(Filters.eq("playerID", playerUUID.toString()))) {
            if (playerDataType == PlayerDataType.INVENTORY)
                playerEncodedData.add((EncodeResult) ((Document) nextDocument.get("data")).get("inventory"));
            else
                playerEncodedData.add((EncodeResult) ((Document) nextDocument.get("data")).get("armor"));
        }

        return playerEncodedData;

    }

    public void closeConnection() {
        this.mongoConnection.close();
    }

}
