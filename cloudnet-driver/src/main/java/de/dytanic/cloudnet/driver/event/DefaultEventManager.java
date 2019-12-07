package de.dytanic.cloudnet.driver.event;

import de.dytanic.cloudnet.common.Validate;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DefaultEventManager implements EventManager {

    //Map<Channel, Listeners>
    private final Map<String, List<RegisteredEventListener>> registeredListeners = new HashMap<>();

    @Override
    public EventManager registerListener(Object listener) {
        Validate.checkNotNull(listener);

        this.registerListener0(listener);
        return this;
    }

    @Override
    public EventManager unregisterListener(Object listener) {
        Validate.checkNotNull(listener);

        for (Map.Entry<String, List<RegisteredEventListener>> listeners : this.registeredListeners.entrySet()) {
            for (RegisteredEventListener registeredEventListener : listeners.getValue()) {
                if (registeredEventListener.getInstance().equals(listener)) {
                    listeners.getValue().remove(registeredEventListener);
                }
            }
        }

        return this;
    }

    @Override
    public EventManager unregisterListener(Class<?> listener) {
        Validate.checkNotNull(listener);

        for (Map.Entry<String, List<RegisteredEventListener>> listeners : this.registeredListeners.entrySet()) {
            for (RegisteredEventListener registeredEventListener : listeners.getValue()) {
                if (registeredEventListener.getInstance().getClass().equals(listener)) {
                    listeners.getValue().remove(registeredEventListener);
                }
            }
        }

        return this;
    }

    @Override
    public EventManager unregisterListeners(ClassLoader classLoader) {
        Validate.checkNotNull(classLoader);

        for (Map.Entry<String, List<RegisteredEventListener>> listeners : this.registeredListeners.entrySet()) {
            for (RegisteredEventListener registeredEventListener : listeners.getValue()) {
                if (registeredEventListener.getInstance().getClass().getClassLoader().equals(classLoader)) {
                    listeners.getValue().remove(registeredEventListener);
                }
            }
        }

        return this;
    }

    @Override
    public EventManager unregisterListeners(Object... listeners) {
        Validate.checkNotNull(listeners);

        for (Object listener : listeners) {
            unregisterListener(listener);
        }

        return this;
    }

    @Override
    public EventManager unregisterListeners(Class<?>... classes) {
        Validate.checkNotNull(classes);

        for (Object listener : classes) {
            unregisterListener(listener);
        }

        return this;
    }

    @Override
    public EventManager unregisterAll() {
        this.registeredListeners.clear();
        return this;
    }

    @Override
    public <T extends Event> T callEvent(String channel, T event) {
        if (channel == null) {
            channel = "*";
        }
        Validate.checkNotNull(event);

        fireEvent(channel, event);
        return event;
    }


    private void fireEvent(String channel, Event event) {
        if (channel.equals("*")) {
            List<RegisteredEventListener> listeners = new ArrayList<>();

            for (List<RegisteredEventListener> entry : this.registeredListeners.values()) {
                listeners.addAll(entry);
            }

            fireEvent0(listeners, event);

        } else if (this.registeredListeners.containsKey(channel)) {
            fireEvent0(this.registeredListeners.get(channel), event);
        }
    }

    private void fireEvent0(List<RegisteredEventListener> listeners, Event event) {
        Collections.sort(listeners);

        for (RegisteredEventListener listener : listeners) {
            listener.fireEvent(event);
        }
    }

    private void registerListener0(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (
                    method.getParameterCount() == 1 &&
                            method.isAnnotationPresent(EventListener.class) &&
                            Event.class.isAssignableFrom(method.getParameters()[0].getType())) {
                EventListener eventListener = method.getAnnotation(EventListener.class);

                RegisteredEventListener registeredEventListener = new DefaultRegisteredEventListener(
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