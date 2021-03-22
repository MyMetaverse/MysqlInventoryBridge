package net.craftersland.bridge.inventory.jedisbridge;

import lombok.Getter;
import net.craftersland.bridge.inventory.encoder.DataRetainer;
import net.craftersland.bridge.inventory.encoder.Encoder;
import net.craftersland.bridge.inventory.encoder.EncoderFactory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.Serializable;

public class BridgeResult implements DataRetainer, Serializable {

    private final String rawInventory;
    private final String rawArmor;

    private final Encoder encoder;

    @Getter
    private final Result result;

    public BridgeResult(String rawInventory, String rawArmor, String codec, Result result) {
        this.rawInventory = rawInventory;
        this.rawArmor = rawArmor;

        this.encoder = EncoderFactory.getEncoder(codec);
        this.result = result;
    }

    public BridgeResult(Result result) {
        this(null, null, null, result);
    }

    public BridgeResult(BridgeTransporter transporter) {
        this(transporter.getRawInventory(), transporter.getRawArmor(), transporter.getCodec(), Result.SAVED);
    }

    public ItemStack[] getInventory() throws IOException {
        if(encoder == null)
            throw new NullPointerException("Encoder required not found.");
        if(rawInventory.equals("none"))
            return null;
        return encoder.decode(rawInventory);
    }

    public ItemStack[] getArmor() throws IOException {
        if(encoder == null)
            throw new NullPointerException("Encoder required not found.");
        if(rawArmor.equals("none"))
            return null;
        return encoder.decode(rawArmor);
    }


    public enum Result {
        SAVED, NOT_SAVED, DONE;
    }
}
