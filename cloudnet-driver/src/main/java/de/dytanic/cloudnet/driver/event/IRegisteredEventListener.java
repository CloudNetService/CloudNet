package de.dytanic.cloudnet.driver.event;

import de.dytanic.cloudnet.driver.event.invoker.ListenerInvoker;

public interface IRegisteredEventListener extends Comparable<IRegisteredEventListener> {

  void fireEvent(Event event);

  EventListener getEventListener();

  EventPriority getPriority();

  Object getInstance();

  ListenerInvoker getInvoker();

  Class<?> getEventClass();

  String getMethodName();

  @Override
  default int compareTo(IRegisteredEventListener other) {
    return this.getPriority().compareTo(other.getPriority());
  }
}
