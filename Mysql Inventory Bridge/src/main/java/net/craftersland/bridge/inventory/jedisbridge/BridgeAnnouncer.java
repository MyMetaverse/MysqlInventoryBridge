package net.craftersland.bridge.inventory.jedisbridge;

import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.events.PlayerJoin;
import org.bukkit.entity.Player;
import redis.clients.jedis.BinaryJedisPubSub;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class BridgeAnnouncer extends BinaryJedisPubSub {

    private final Main main;

    public BridgeAnnouncer(Main main) {
        this.main = main;
    }

    @Override
    public void onMessage(byte[] channel, byte[] message) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(message)) {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                BridgeTransporter bridgeTransporter = (BridgeTransporter) objectInputStream.readObject();
                if (bridgeTransporter != null) {
                    main.getInventoryDataHandler().assignPlayer(bridgeTransporter.getUniqueId(),
                            new BridgeResult(bridgeTransporter));
                    Player player = main.getServer().getPlayer(bridgeTransporter.getUniqueId());
                    if (player != null && player.isOnline()) {
                        main.getInventoryDataHandler().onJoinFunction(player, true);

                        PlayerJoin.queue.computeIfPresent(bridgeTransporter.getUniqueId(), (uuid, bridgeMonitor) -> {
                            bridgeMonitor.recall();
                            main.getBridge().loadedFromMessage.add(uuid);
                            return null;
                        });

                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
