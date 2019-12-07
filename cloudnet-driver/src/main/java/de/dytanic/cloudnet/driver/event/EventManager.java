package de.dytanic.cloudnet.driver.event;

import de.dytanic.cloudnet.common.Validate;

public interface EventManager {

    EventManager registerListener(Object listener);

    EventManager unregisterListener(Object listener);

    EventManager unregisterListener(Class<?> listener);

    EventManager unregisterListeners(ClassLoader classLoader);

    EventManager unregisterListeners(Object... listeners);

    EventManager unregisterListeners(Class<?>... classes);

    EventManager unregisterAll();

    <T extends Event> T callEvent(String channel, T event);


    default <T extends Event> T callEvent(T event) {
        return this.callEvent("*", event);
    }

    default EventManager registerListeners(Object... listeners) {
        Validate.checkNotNull(listeners);

        for (Object listener : listeners) {
            this.registerListener(listener);
        }

        return this;
    }
}