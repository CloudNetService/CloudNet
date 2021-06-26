/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.driver.event;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.invoker.ListenerInvoker;
import de.dytanic.cloudnet.driver.event.invoker.ListenerInvokerGenerator;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DefaultEventManager implements IEventManager {

  private final Map<String, List<IRegisteredEventListener>> registeredListeners = new HashMap<>();

  private final ListenerInvokerGenerator invokerGenerator = new ListenerInvokerGenerator();

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
      listeners.getValue()
        .removeIf(registeredEventListener -> registeredEventListener.getInstance().getClass().equals(listener));
    }

    return this;
  }

  @Override
  public IEventManager unregisterListeners(ClassLoader classLoader) {
    Preconditions.checkNotNull(classLoader);

    for (Map.Entry<String, List<IRegisteredEventListener>> listeners : this.registeredListeners.entrySet()) {
      listeners.getValue().removeIf(
        registeredEventListener -> registeredEventListener.getInstance().getClass().getClassLoader()
          .equals(classLoader));
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

      this.fireEvent(listeners, event);
    } else if (this.registeredListeners.containsKey(channel)) {
      this.fireEvent(this.registeredListeners.get(channel), event);
    }
  }

  private void fireEvent(List<IRegisteredEventListener> listeners, Event event) {
    Collections.sort(listeners);

    for (IRegisteredEventListener listener : listeners) {
      listener.fireEvent(event);
    }
  }

  @SuppressWarnings("unchecked")
  private void registerListener0(Object listener) {
    for (Method method : listener.getClass().getMethods()) {
      if (!method.isAnnotationPresent(EventListener.class)) {
        continue;
      }

      if (method.getParameterCount() != 1 || !Modifier.isPublic(method.getModifiers())) {
        throw new IllegalStateException(String.format(
          "Listener method %s:%s has to be public with exactly one argument",
          listener.getClass().getName(),
          method.getName()));
      }

      Class<?> parameterType = method.getParameters()[0].getType();

      if (!Event.class.isAssignableFrom(parameterType)) {
        throw new IllegalStateException(String.format(
          "Parameter type %s of listener method %s:%s is not an event",
          parameterType.getName(),
          listener.getClass().getName(),
          method.getName()));
      }

      Class<Event> eventClass = (Class<Event>) parameterType;
      String methodName = method.getName();

      EventListener eventListener = method.getAnnotation(EventListener.class);
      ListenerInvoker listenerInvoker = this.invokerGenerator.generate(listener, methodName, eventClass);

      IRegisteredEventListener registeredEventListener = new DefaultRegisteredEventListener(
        eventListener,
        eventListener.priority(),
        listener,
        eventClass,
        methodName,
        listenerInvoker);

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

      this.registeredListeners.computeIfAbsent(eventListener.channel(),
        key -> new CopyOnWriteArrayList<>()).add(registeredEventListener);
    }
  }
}
