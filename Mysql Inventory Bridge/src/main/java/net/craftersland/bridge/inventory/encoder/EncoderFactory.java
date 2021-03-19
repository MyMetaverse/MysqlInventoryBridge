package net.craftersland.bridge.inventory.encoder;

public class EncoderFactory {

    public static Encoder getEncoder(String type) {

        if (type.equals("vanilla")) {
            return new VanillaEncoder();
        } else if(type.equals("modded")) {
            return new ModdedEncoder();
        }

        return null;
    }
}
