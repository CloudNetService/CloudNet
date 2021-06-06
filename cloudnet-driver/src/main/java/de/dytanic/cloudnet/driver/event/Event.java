package de.dytanic.cloudnet.driver.event;

/**
 * The root class for all events, which you want to fire with the EventManager implementations. It is also meant to make
 * it possible for listeners to have access to every object of every subclass.
 *
 * @see IEventManager
 */
public abstract class Event {

  public boolean isShowDebug() {
    return true;
  }

}
