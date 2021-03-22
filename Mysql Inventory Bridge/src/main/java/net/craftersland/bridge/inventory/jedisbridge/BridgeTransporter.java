package net.craftersland.bridge.inventory.jedisbridge;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class BridgeTransporter implements Serializable {

    private final UUID uniqueId;

    private final String rawInventory;
    private final String rawArmor;

    private final String codec;

    private final int heldSlot;



}
