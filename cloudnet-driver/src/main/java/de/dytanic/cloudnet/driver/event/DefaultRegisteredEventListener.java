package de.dytanic.cloudnet.driver.event;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.invoker.ListenerInvoker;

public class DefaultRegisteredEventListener implements IRegisteredEventListener {

    private final EventListener eventListener;
    private final EventPriority priority;
    private final Object instance;
    private final Class<?> eventClass;
    private final String methodName;
    private final ListenerInvoker listenerInvoker;

    public DefaultRegisteredEventListener(
            EventListener eventListener,
            EventPriority priority,
            Object instance,
            Class<? extends Event> eventClass,
            String methodName,
            ListenerInvoker listenerInvoker) {
        this.eventListener = eventListener;
        this.priority = priority;
        this.instance = instance;
        this.eventClass = eventClass;
        this.methodName = methodName;
        this.listenerInvoker = listenerInvoker;
    }

    @Override
    public void fireEvent(Event event) {
        Preconditions.checkNotNull(event);

        if (!this.getEventClass().isAssignableFrom(event.getClass())) {
            return;
        }

        if (event.isShowDebug()) {
            CloudNetDriver.optionalInstance().ifPresent(cloudNetDriver -> {
                if (cloudNetDriver.getLogger().getLevel() >= LogLevel.DEBUG.getLevel()) {
                    cloudNetDriver.getLogger().debug(String.format(
                            "Calling event %s on listener %s",
                            event.getClass().getName(),
                            this.getInstance().getClass().getName()
                    ));
                }
            });
        }

        try {
            this.listenerInvoker.invoke(event);
        } catch (Exception exception) {
            throw new EventListenerException(String.format(
                    "Error while invoking event listener %s in class %s",
                    this.methodName,
                    this.instance.getClass().getName()), exception);
        }
    }

    @Override
    public EventListener getEventListener() {
        return this.eventListener;
    }

    @Override
    public EventPriority getPriority() {
        return this.priority;
    }

    @Override
    public Object getInstance() {
        return this.instance;
    }

    @Override
    public Class<?> getEventClass() {
        return this.eventClass;
    }

    @Override
    public String getMethodName() {
        return this.methodName;
    }

    @Override
    public ListenerInvoker getInvoker() {
        return this.listenerInvoker;
    }
}