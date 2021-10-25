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

import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import de.dytanic.cloudnet.driver.event.invoker.ListenerInvokerGenerator;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

public class DefaultEventManager implements IEventManager {

  /**
   * Holds all registered events mapped to all registered events listeners
   */
  protected final SetMultimap<Class<?>, IRegisteredEventListener> listeners = Multimaps.newSortedSetMultimap(
    new ConcurrentHashMap<>(),
    TreeSet::new);

  @Override
  public @NotNull IEventManager unregisterListeners(@NotNull ClassLoader classLoader) {
    for (Entry<Class<?>, IRegisteredEventListener> entry : this.listeners.entries()) {
      if (entry.getValue().getInstance().getClass().getClassLoader().equals(classLoader)) {
        this.listeners.remove(entry.getKey(), entry.getValue());
      }
    }
    // for chaining
    return this;
  }

  @Override
  public @NotNull IEventManager unregisterListener(Object @NotNull ... listeners) {
    for (Entry<Class<?>, IRegisteredEventListener> entry : this.listeners.entries()) {
      if (Arrays.stream(listeners).anyMatch(instance -> instance.equals(entry.getValue().getInstance()))) {
        this.listeners.remove(entry.getKey(), entry.getValue());
      }
    }
    // for chaining
    return this;
  }

  @Override
  public <T extends Event> @NotNull T callEvent(@NotNull String channel, @NotNull T event) {
    // get all registered listeners of the event
    Set<IRegisteredEventListener> listeners = this.listeners.get(event.getClass());
    if (!listeners.isEmpty()) {
      // post the event to the listeners
      for (IRegisteredEventListener listener : listeners) {
        // check if the event gets called on the same channel as the listener is listening to
        if (listener.getChannel().equals(channel)) {
          listener.fireEvent(event);
        }
      }
    }
    // for chaining
    return event;
  }

  @Override
  public @NotNull IEventManager registerListener(@NotNull Object listener) {
    // get all methods of the listener
    for (Method method : listener.getClass().getDeclaredMethods()) {
      // check if the method can be used
      EventListener annotation = method.getAnnotation(EventListener.class);
      if (annotation != null && method.getParameterCount() == 1) {
        // check the parameter type
        Class<?> eventClass = method.getParameterTypes()[0];
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
