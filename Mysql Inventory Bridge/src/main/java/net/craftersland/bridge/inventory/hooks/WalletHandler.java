package net.craftersland.bridge.inventory.hooks;

import io.mymetaverse.livewallet.api.MetaWalletAPI;
import io.mymetaverse.livewallet.data.tokens.RegisteredToken;
import io.mymetaverse.livewallet.utils.TabType;
import net.craftersland.bridge.inventory.objects.BlackListedItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class WalletHandler {

    private final Plugin walletPlugin;

    public WalletHandler(Plugin walletPlugin) {
        this.walletPlugin = walletPlugin;
    }

    public Set<BlackListedItem> getBlacklistItems() {

        if (!this.getStatus()) return null;

        Set<RegisteredToken> registeredWeapons = MetaWalletAPI.getInstance().getSpecificTokens(TabType.EQUIPMENT);
        Set<BlackListedItem> blackListedItems = new HashSet<>();

        for (RegisteredToken registeredWeapon : registeredWeapons) {

            if (registeredWeapon.getMaterial() == null) continue;

            if (registeredWeapon.getItemModelData() == null) continue;

            if (!registeredWeapon.getItemModelData().matches("[0-9.]+")) continue;

            ItemStack weaponItem = new ItemStack(registeredWeapon.getMaterial());
            ItemMeta weaponItemMeta = weaponItem.getItemMeta();
            int weaponCustomModelData = (int)Double.parseDouble(registeredWeapon.getItemModelData());

            if (weaponItemMeta == null) continue;

            weaponItemMeta.setCustomModelData(weaponCustomModelData);
            weaponItem.setItemMeta(weaponItemMeta);

            blackListedItems.add(new BlackListedItem(weaponItem));

        }

        return blackListedItems;

    }

    public boolean getStatus() {
        if (this.walletPlugin == null) return false;
        return this.walletPlugin.isEnabled();
    }

}
