package de.dytanic.cloudnet.driver.event;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DefaultEventManager implements IEventManager {

    //Map<Channel, Listeners>
    private final Map<String, List<IRegisteredEventListener>> registeredListeners = new HashMap<>();

    @Override
    public IEventManager registerListener(Object listener) {
        Preconditions.checkNotNull(listener);

        this.registerListener0(listener);
        return this;
    }

    @Override
    public IEventManager unregisterListener(Object listener) {
        Preconditions.checkNotNull(listener);

        for (Map.Entry<String, List<IRegisteredEventListener>> listeners : this.registeredListeners.entrySet()) {
            listeners.getValue().removeIf(registeredEventListener -> registeredEventListener.getInstance().equals(listener));
        }

        return this;
    }

    @Override
    public IEventManager unregisterListener(Class<?> listener) {
        Preconditions.checkNotNull(listener);

        for (Map.Entry<String, List<IRegisteredEventListener>> listeners : this.registeredListeners.entrySet()) {
            listeners.getValue().removeIf(registeredEventListener -> registeredEventListener.getInstance().getClass().equals(listener));
        }

        return this;
    }

    @Override
    public IEventManager unregisterListeners(ClassLoader classLoader) {
        Preconditions.checkNotNull(classLoader);

        for (Map.Entry<String, List<IRegisteredEventListener>> listeners : this.registeredListeners.entrySet()) {
            listeners.getValue().removeIf(registeredEventListener -> registeredEventListener.getInstance().getClass().getClassLoader().equals(classLoader));
        }

        return this;
    }

    @Override
    public IEventManager unregisterListeners(Object... listeners) {
        Preconditions.checkNotNull(listeners);

        for (Object listener : listeners) {
            this.unregisterListener(listener);
        }

        return this;
    }

    @Override
    public IEventManager unregisterListeners(Class<?>... classes) {
        Preconditions.checkNotNull(classes);

        for (Object listener : classes) {
            this.unregisterListener(listener);
        }

        return this;
    }

    @Override
    public IEventManager unregisterAll() {
        this.registeredListeners.clear();
        return this;
    }

    @Override
    public <T extends Event> T callEvent(String channel, T event) {
        if (channel == null) {
            channel = "*";
        }
        Preconditions.checkNotNull(event);

        this.fireEvent(channel, event);
        return event;
    }


    private void fireEvent(String channel, Event event) {
        if (channel.equals("*")) {
            List<IRegisteredEventListener> listeners = new ArrayList<>();

            for (List<IRegisteredEventListener> entry : this.registeredListeners.values()) {
                listeners.addAll(entry);
            }

            this.fireEvent0(listeners, event);

        } else if (this.registeredListeners.containsKey(channel)) {
            this.fireEvent0(this.registeredListeners.get(channel), event);
        }
    }

    private void fireEvent0(List<IRegisteredEventListener> listeners, Event event) {
        Collections.sort(listeners);

        for (IRegisteredEventListener listener : listeners) {
            listener.fireEvent(event);
        }
    }

    private void registerListener0(Object listener) {
        for (Method method : listener.getClass().getMethods()) {
            CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> {
                if (cloudNetDriver.getLogger().getLevel() >= LogLevel.DEBUG.getLevel()) {
                    cloudNetDriver.getLogger().debug(String.format(
                            "Registering listener method %s:%s from class loader %s",
                            listener.getClass().getName(),
                            method.getName(),
                            listener.getClass().getClassLoader().getClass().getName()
                    ));
                }
            });
            if (
                    method.getParameterCount() == 1 &&
                            method.isAnnotationPresent(EventListener.class) &&
                            Event.class.isAssignableFrom(method.getParameters()[0].getType())) {
                EventListener eventListener = method.getAnnotation(EventListener.class);

                IRegisteredEventListener registeredEventListener = new DefaultRegisteredEventListener(
                        eventListener,
                        eventListener.priority(),
                        listener,
                        method,
                        (Class<? extends Event>) method.getParameters()[0].getType()
                );

                if (!this.registeredListeners.containsKey(eventListener.channel())) {
                    this.registeredListeners.put(eventListener.channel(), new CopyOnWriteArrayList<>());
                }

                this.registeredListeners.get(eventListener.channel()).add(registeredEventListener);
            }
        }
    }
}