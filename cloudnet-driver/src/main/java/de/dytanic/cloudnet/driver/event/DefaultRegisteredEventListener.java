package de.dytanic.cloudnet.driver.event;

import java.lang.reflect.Method;

public class DefaultRegisteredEventListener implements IRegisteredEventListener {

    protected EventListener eventListener;

    protected EventPriority priority;

    protected Object instance;

    protected Method handlerMethod;

    protected Class<? extends Event> eventClass;

    public DefaultRegisteredEventListener(EventListener eventListener, EventPriority priority, Object instance, Method handlerMethod, Class<? extends Event> eventClass) {
        this.eventListener = eventListener;
        this.priority = priority;
        this.instance = instance;
        this.handlerMethod = handlerMethod;
        this.eventClass = eventClass;
    }

    public EventListener getEventListener() {
        return this.eventListener;
    }

    public EventPriority getPriority() {
        return this.priority;
    }

    public Object getInstance() {
        return this.instance;
    }

    public Method getHandlerMethod() {
        return this.handlerMethod;
    }

    public Class<? extends Event> getEventClass() {
        return this.eventClass;
    }
}