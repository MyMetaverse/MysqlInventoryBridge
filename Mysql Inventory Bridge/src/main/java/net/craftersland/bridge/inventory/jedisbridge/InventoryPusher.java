package net.craftersland.bridge.inventory.jedisbridge;

import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.encoder.EncodeResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class InventoryPusher {

    private final Main main;

    public InventoryPusher(Main main) {
        this.main = main;
    }

    public void addToQueue(Player pl) {
        Mono.just(pl)
                .map(player -> {
                    ItemStack[] inventory = main.getInventoryDataHandler().getInventory(player);
                    ItemStack[] armor = main.getInventoryDataHandler().getArmor(player);
                    return new Object[] {
                            player.getUniqueId(),
                            main.getInventoryDataHandler().encodeItems(inventory),
                            main.getInventoryDataHandler().encodeItems(armor)
                    };
                })
                .doOnNext(res ->
                    main.getBridge().cachePlayer(
                            (UUID) res[0],
                            (EncodeResult) res[1], // We encode data according to our configuration.
                            (EncodeResult) res[2],
                            false
                    )
                )
                .subscribe();
    }

}
