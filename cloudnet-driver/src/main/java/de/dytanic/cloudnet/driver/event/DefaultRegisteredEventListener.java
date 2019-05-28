package de.dytanic.cloudnet.driver.event;

import java.lang.reflect.Method;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DefaultRegisteredEventListener implements
    IRegisteredEventListener {

  protected EventListener eventListener;

  protected EventPriority priority;

  protected Object instance;

  protected Method handlerMethod;

  protected Class<? extends Event> eventClass;

}