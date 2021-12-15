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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import de.dytanic.cloudnet.driver.event.invoker.ListenerInvokerGenerator;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;

public class DefaultEventManager implements IEventManager {

  /**
   * Holds all registered events mapped to all registered events listeners
   */
  protected final ListMultimap<Class<?>, IRegisteredEventListener> listeners = Multimaps.newListMultimap(
    new ConcurrentHashMap<>(),
    CopyOnWriteArrayList::new);

  @Override
  public @NotNull IEventManager unregisterListeners(@NotNull ClassLoader classLoader) {
    for (var entry : this.listeners.entries()) {
      if (entry.getValue().instance().getClass().getClassLoader().equals(classLoader)) {
        this.listeners.remove(entry.getKey(), entry.getValue());
      }
    }
    // for chaining
    return this;
  }

  @Override
  public @NotNull IEventManager unregisterListener(Object @NotNull ... listeners) {
    for (var entry : this.listeners.entries()) {
      if (Arrays.stream(listeners).anyMatch(instance -> instance.equals(entry.getValue().instance()))) {
        this.listeners.remove(entry.getKey(), entry.getValue());
      }
    }
    // for chaining
    return this;
  }

  @Override
  public <T extends Event> @NotNull T callEvent(@NotNull String channel, @NotNull T event) {
    // get all registered listeners of the event
    var listeners = this.listeners.get(event.getClass());
    if (!listeners.isEmpty()) {
      // check if there is only one listener
      if (listeners.size() == 1) {
        var listener = listeners.get(0);
        // check if the event gets called on the same channel as the listener is listening to
        if (listener.channel().equals(channel)) {
          listener.fireEvent(event);
        }
      } else {
        // workaround to sort the list to keep concurrency (Collections.sort isn't working here)
        var targets = listeners.toArray(new IRegisteredEventListener[0]);
        Arrays.sort(targets);
        // post the event to the listeners
        for (var listener : targets) {
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
  public @NotNull IEventManager registerListener(@NotNull Object listener) {
    // get all methods of the listener
    for (var method : listener.getClass().getDeclaredMethods()) {
      // check if the method can be used
      var annotation = method.getAnnotation(EventListener.class);
      if (annotation != null && method.getParameterCount() == 1) {
        // check the parameter type
        var eventClass = method.getParameterTypes()[0];
        if (!Event.class.isAssignableFrom(eventClass)) {
          throw new IllegalStateException(String.format(
            "Parameter type %s (index 0) of listener method %s in %s is not a subclass of Event",
            eventClass.getName(),
            method.getName(),
            listener.getClass().getName()));
        }
        // register the listener
        this.listeners.put(eventClass, new DefaultRegisteredEventListener(
          listener,
          method.getName(),
          eventClass,
          annotation,
          ListenerInvokerGenerator.generate(listener, method, eventClass)));
      }
    }
    // for chaining
    return this;
  }
}
