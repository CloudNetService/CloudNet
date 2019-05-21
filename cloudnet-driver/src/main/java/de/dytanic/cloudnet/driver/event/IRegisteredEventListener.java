package de.dytanic.cloudnet.driver.event;

import de.dytanic.cloudnet.common.Validate;

import java.lang.reflect.Method;

public interface IRegisteredEventListener extends Comparable<IRegisteredEventListener> {

    EventListener getEventListener();

    EventPriority getPriority();

    Object getInstance();

    Method getHandlerMethod();

    Class<? extends Event> getEventClass();

    default <T extends Event> T fireEvent(T event)
    {
        Validate.checkNotNull(event);

        if (getEventClass().isAssignableFrom(event.getClass()))
            try
            {
                getHandlerMethod().setAccessible(true);
                getHandlerMethod().invoke(getInstance(), event);
            } catch (Exception ex)
            {
                throw new EventListenerException("An error on offerTask method " + getHandlerMethod().getName() + " in class " + getInstance().getClass(), ex);
            }

        return event;
    }

    @Override
    default int compareTo(IRegisteredEventListener o)
    {
        return this.getPriority().compareTo(o.getPriority());
    }
}