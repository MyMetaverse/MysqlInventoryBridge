package net.craftersland.bridge.inventory.jedisbridge;

import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.encoder.EncodeResult;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Bridge {

    private static final SetParams TIMEOUT = new SetParams().ex(500L); // We set the cache to 800 seconds.

    private final Main main;

    JedisPool jedisPool;

    public final Set<UUID> loadedFromMessage = new HashSet<>();

    public Bridge(Main main) {
        this.main = main;
        connect();

        MinecraftRedisListener minecraftRedisListener = new MinecraftRedisListener(main.getInventoryPusher());

        main.getServer().getPluginManager().registerEvents(minecraftRedisListener, main);

        BridgeAnnouncer bridgeAnnouncer = new BridgeAnnouncer(main);

        new BukkitRunnable() {
            @Override
            public void run() {
                jedisPool.getResource().subscribe(bridgeAnnouncer, "mib".getBytes(StandardCharsets.UTF_8));
            }
        }.runTaskAsynchronously(main);

    }

    public void connect() {
        this.jedisPool = new JedisPool(new JedisPoolConfig(), main.getConfigHandler().getString("redis.host", "localhost"));
    }

    public void closeRedis() {
        jedisPool.close();
        jedisPool.destroy();
    }

    /**
     * We attempt to read the player from memory (if is registered)
     * @param uniqueId The player
     * @return The result of the read.
     */
    public BridgeResult readPlayer(UUID uniqueId) {
        try (Jedis jedis = this.jedisPool.getResource()) {

            if(loadedFromMessage.remove(uniqueId))
                return new BridgeResult(BridgeResult.Result.DONE);

            // We first check if the player is in memory.
            if(!jedis.exists("mibPlayer:" + uniqueId.toString())) // This key will be saved when a player joins the server.
                return new BridgeResult(BridgeResult.Result.NOT_SAVED); // If is not saved, we need to get data from SQL.

            // If the player exists we'll use the data inside our bridge object.
            return new BridgeResult(
                    jedis.get("mibPlayer:inventory:" + uniqueId),
                    jedis.get("mibPlayer:armor:" + uniqueId),
                    jedis.get("mibPlayer:codec:" + uniqueId),
                    BridgeResult.Result.SAVED
            );
        }

    }

    /**
     * We save our player into memory (redis).
     * @param uniqueId The player
     * @param inventory The inventory encoded.
     * @param armor The armor encoded.
     */
    public void cachePlayer(UUID uniqueId, EncodeResult inventory, EncodeResult armor, boolean announce, Player player) {
        try (Jedis jedis = jedisPool.getResource()) {

            jedis.set("mibPlayer:codec:" + uniqueId.toString(), inventory.getCodec(), TIMEOUT);
            jedis.set("mibPlayer:inventory:" + uniqueId, inventory.getResult(), TIMEOUT);
            jedis.set("mibPlayer:armor:" + uniqueId, armor.getResult(), TIMEOUT);

            registerPlayer(uniqueId);

            if (announce) {
                try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    try (ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                        outputStream.writeObject(new BridgeTransporter(
                                uniqueId,
                                inventory.getResult(),
                                armor.getResult(),
                                inventory.getCodec(),
                                player != null ? player.getInventory().getHeldItemSlot() : -1
                        ));
                        outputStream.flush();
                        jedis.publish("mib".getBytes(StandardCharsets.UTF_8), byteArrayOutputStream.toByteArray());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * We register the player to know that we can now use their inventory.
     * @param uniqueId The player to be register.
     */
    public void registerPlayer(UUID uniqueId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set("mibPlayer:" + uniqueId.toString(), "y", TIMEOUT);
        }
    }


}
