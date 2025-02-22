/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.impl.event;

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.RegisteredEventListener;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import jakarta.inject.Singleton;
import java.lang.reflect.Modifier;
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

/**
 * The default implementation of an event manager.
 *
 * @since 4.0
 */
@Singleton
@Provides(EventManager.class)
public final class DefaultEventManager implements EventManager {

  protected final Lock bakeLock = new ReentrantLock(true);
  protected final Map<Class<?>, List<RegisteredEventListener>> listeners = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull EventManager unregisterListeners(@NonNull ClassLoader classLoader) {
    this.safeRemove(value -> value.instance().getClass().getClassLoader().equals(classLoader));
    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull EventManager unregisterListener(Object @NonNull ... listeners) {
    var listenerList = Arrays.asList(listeners);
    this.safeRemove(value -> listenerList.contains(value.instance()));
    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull EventManager registerListener(@NonNull Class<?> listenerClass) {
    var injectionLayer = InjectionLayer.findLayerOf(listenerClass);
    return this.registerListener(injectionLayer, injectionLayer.instance(listenerClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull EventManager registerListener(@NonNull Object listener) {
    var injectionLayer = InjectionLayer.findLayerOf(listener);
    return this.registerListener(injectionLayer, listener);
  }

  /**
   * Registers all methods in the given listener class which are annotated with {@link EventListener} and are taking
   * only one argument with a subtype of {@link Event}.
   * <p>
   * This method accepts public, protected, default (package) access, and private methods but will not include inherited
   * methods at all.
   * <p>
   * All methods which are not annotated with {@link EventListener}, are static and are not taking one or more arguments
   * are silently ignored.
   *
   * @param layer    the injection layer to use to get instances needed for event listeners.
   * @param listener the instance of the listener to register the methods in.
   * @return the same event manager as used to call the method, for chaining.
   * @throws NullPointerException     if the given listener or injection layer is null.
   * @throws IllegalArgumentException if an event listener target doesn't take an event as it's first argument.
   */
  protected @NonNull EventManager registerListener(@NonNull InjectionLayer<?> layer, @NonNull Object listener) {
    // get all methods of the listener
    for (var method : listener.getClass().getDeclaredMethods()) {
      // check if the method can be used
      var annotation = method.getAnnotation(EventListener.class);
      if (annotation != null && !Modifier.isStatic(method.getModifiers()) && method.getParameterCount() >= 1) {
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
        var eventListener = new DefaultRegisteredEventListener(listener, method, annotation, layer);

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

  /**
   * Safely removes the all registered event listeners which are matching the given predicate.
   *
   * @param predicate the predicate all listeners to remove must match.
   * @throws NullPointerException if the given predicate is null.
   */
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
