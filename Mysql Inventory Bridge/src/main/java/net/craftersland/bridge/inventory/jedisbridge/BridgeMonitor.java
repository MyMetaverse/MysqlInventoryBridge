package net.craftersland.bridge.inventory.jedisbridge;

import lombok.SneakyThrows;

public class BridgeMonitor {

    @SneakyThrows
    public void call() {
        synchronized (this) {
            wait(200);
        }
    }

    public void recall() {
        synchronized (this) {
            notify();
        }
    }

}



