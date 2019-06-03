package de.dytanic.cloudnet.driver.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Method;

@Getter
@AllArgsConstructor
public class DefaultRegisteredEventListener implements IRegisteredEventListener {

    protected EventListener eventListener;

    protected EventPriority priority;

    protected Object instance;

    protected Method handlerMethod;

    protected Class<? extends Event> eventClass;

}