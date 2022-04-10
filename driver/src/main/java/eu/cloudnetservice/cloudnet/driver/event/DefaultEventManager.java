/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.event;

import eu.cloudnetservice.cloudnet.driver.event.invoker.ListenerInvokerGenerator;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import lombok.NonNull;

public class DefaultEventManager implements EventManager {

  protected final Lock bakeLock = new ReentrantLock(true);
  protected final Map<Class<?>, List<RegisteredEventListener>> listeners = new HashMap<>();

  @Override
  public @NonNull EventManager unregisterListeners(@NonNull ClassLoader classLoader) {
    this.safeRemove(value -> value.instance().getClass().getClassLoader().equals(classLoader));
    // for chaining
    return this;
  }

  @Override
  public @NonNull EventManager unregisterListener(Object @NonNull ... listeners) {
    var listenerList = Arrays.asList(listeners);
    this.safeRemove(value -> listenerList.contains(value.instance()));
    // for chaining
    return this;
  }

  @Override
  public <T extends Event> @NonNull T callEvent(@NonNull String channel, @NonNull T event) {
    // get all registered listeners of the event
    var listeners = this.listeners.get(event.getClass());
    if (listeners != null && !listeners.isEmpty()) {
      // check if there is only one listener
      if (listeners.size() == 1) {
        var listener = listeners.get(0);
        // check if the event gets called on the same channel as the listener is listening to
        if (listener.channel().equals(channel)) {
          listener.fireEvent(event);
        }
      } else {
        // post the event to the listeners
        for (var listener : listeners) {
          // check if the event gets called on the same channel as the listener is listening to
          if (listener.channel().equals(channel)) {
            listener.fireEvent(event);
          }
        }
      }
    }
    // for chaining
    return event;
  }

  @Override
  public @NonNull EventManager registerListener(@NonNull Object listener) {
    // get all methods of the listener
    for (var method : listener.getClass().getDeclaredMethods()) {
      // check if the method can be used
      var annotation = method.getAnnotation(EventListener.class);
      if (annotation != null && method.getParameterCount() == 1) {
        // check the parameter type
        var eventClass = method.getParameterTypes()[0];
        if (!Event.class.isAssignableFrom(eventClass)) {
          throw new IllegalArgumentException(String.format(
            "Parameter type %s (index 0) of listener method %s in %s is not a subclass of Event",
            eventClass.getName(),
            method.getName(),
            listener.getClass().getName()));
        }
        // bring the information together
        var eventListener = new DefaultRegisteredEventListener(
          listener,
          method.getName(),
          eventClass,
          annotation,
          ListenerInvokerGenerator.generate(listener, method, eventClass));

        this.bakeLock.lock();
        try {
          // bake an event listener from the information
          var listeners = this.listeners.computeIfAbsent(eventClass, $ -> new CopyOnWriteArrayList<>());
          listeners.add(eventListener);
          // sort now - we don't need to sort lather then
          Collections.sort(listeners);
        } finally {
          this.bakeLock.unlock();
        }
      }
    }
    // for chaining
    return this;
  }

  protected void safeRemove(@NonNull Predicate<RegisteredEventListener> predicate) {
    this.bakeLock.lock();
    try {
      var iterator = this.listeners.values().iterator();
      while (iterator.hasNext()) {
        // remove all listeners which are matching the predicate
        var entry = iterator.next();
        entry.removeIf(predicate);
        // check if the entry is still needed
        if (entry.isEmpty()) {
          iterator.remove();
        }
      }
    } finally {
      this.bakeLock.unlock();
    }
  }
}
