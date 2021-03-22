package net.craftersland.bridge.inventory.encoder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Getter
@RequiredArgsConstructor
public class EncodeResult implements Serializable {

    private final String result;
    private final String codec;

}
