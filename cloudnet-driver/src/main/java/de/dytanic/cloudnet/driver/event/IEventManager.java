package de.dytanic.cloudnet.driver.event;

import com.google.common.base.Preconditions;

public interface IEventManager {

  IEventManager registerListener(Object listener);

  IEventManager unregisterListener(Object listener);

  IEventManager unregisterListener(Class<?> listener);

  IEventManager unregisterListeners(ClassLoader classLoader);

  IEventManager unregisterListeners(Object... listeners);

  IEventManager unregisterListeners(Class<?>... classes);

  IEventManager unregisterAll();

  <T extends Event> T callEvent(String channel, T event);

  default <T extends Event> T callEvent(T event) {
    return this.callEvent("*", event);
  }

  default IEventManager registerListeners(Object... listeners) {
    Preconditions.checkNotNull(listeners);

    for (Object listener : listeners) {
      this.registerListener(listener);
    }

    return this;
  }
}
