package net.craftersland.bridge.inventory.encoder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EncodeResult {

    private final String result;
    private final String codec;

}
