package de.dytanic.cloudnet.driver.event;

import java.util.Comparator;

public enum EventPriority implements Comparator<EventPriority> {

    HIGHEST(128),
    HIGH(64),
    NORMAL(32),
    LOW(16),
    LOWEST(8);

    private int value;

    private EventPriority(int value) {
        this.value = value;
    }

    @Override
    public int compare(EventPriority o1, EventPriority o2) {
        return o1.value - o2.value;
    }

    public int getValue() {
        return this.value;
    }
}