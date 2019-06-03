package de.dytanic.cloudnet.driver.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Comparator;

@Getter
@AllArgsConstructor
public enum EventPriority implements Comparator<EventPriority> {

    HIGHEST(128),
    HIGH(64),
    NORMAL(32),
    LOW(16),
    LOWEST(8);

    private int value;

    @Override
    public int compare(EventPriority o1, EventPriority o2) {
        return o1.value - o2.value;
    }
}