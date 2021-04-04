package net.craftersland.bridge.inventory.objects;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class BlackListedItem {

    private Material material;
    private int customDataId;

    public BlackListedItem(ItemStack itemStack) {
        if (itemStack != null && itemStack.getItemMeta() != null) {
            this.material = itemStack.getType();
            if (itemStack.getItemMeta().hasCustomModelData()) {
                this.customDataId = itemStack.getItemMeta().getCustomModelData();
            }
        }
    }

    public BlackListedItem load(String itemRepresentation) {

        String[] itemRepresentationSplit = itemRepresentation.split(";");

        if (itemRepresentationSplit.length != 2) return null;

        if (!itemRepresentationSplit[1].matches("[0-9]+")) return null;

        Material itemMaterial = Material.matchMaterial(itemRepresentationSplit[0]);
        int customDataId = Integer.parseInt(itemRepresentationSplit[1]);

        if (itemMaterial == null) return null;

        this.material = itemMaterial;
        this.customDataId = customDataId;

        return this;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlackListedItem that = (BlackListedItem) o;
        return customDataId == that.customDataId && material == that.material;
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, customDataId);
    }

    @Override
    public String toString() {
        return "BlackListedItem{" +
                "material=" + material +
                ", customDataId=" + customDataId +
                '}';
    }

}
