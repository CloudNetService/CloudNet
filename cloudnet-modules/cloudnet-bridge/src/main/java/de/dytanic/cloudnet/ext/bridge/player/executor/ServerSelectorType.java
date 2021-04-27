package de.dytanic.cloudnet.ext.bridge.player.executor;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;

import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

public enum ServerSelectorType {

    HIGHEST_PLAYERS((o1, o2) -> Integer.compare(o1.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0), o2.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0))),
    LOWEST_PLAYERS((o1, o2) -> Integer.compare(o2.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0), o1.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0))),
    RANDOM(Comparator.comparingInt(value -> ThreadLocalRandom.current().nextInt(2) - 1));

    private final Comparator<ServiceInfoSnapshot> comparator;

    ServerSelectorType(Comparator<ServiceInfoSnapshot> comparator) {
        this.comparator = comparator;
    }

    public Comparator<ServiceInfoSnapshot> getComparator() {
        return this.comparator;
    }
}
