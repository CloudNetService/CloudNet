package de.dytanic.cloudnet.driver.event.invoker;

import de.dytanic.cloudnet.driver.event.Event;

/**
 * Responsible for invoking event listener methods without reflection.
 * An implementation is automatically generated for every event listener method when registered.
 */
public interface ListenerInvoker {

    /**
     * Invokes the event listener method.
     *
     * @param event The event the listener method should be invoked with.
     *              Passing an event with the wrong type will result in an exception
     */
    void invoke(Event event);
}
