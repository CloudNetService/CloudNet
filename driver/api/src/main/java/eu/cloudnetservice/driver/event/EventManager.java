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

package eu.cloudnetservice.driver.event;

import lombok.NonNull;

/**
 * Dispatches events to listeners, and provides ways for listeners to register themselves.
 * <p>
 * An event manager allows users to publish events to previously registered listeners. The modular style allows plugins
 * or modules to hook and unhook into/from the system dynamically. It is not designed for inter-process communication.
 * <p>
 * To receive an event a listener must:
 * <ol>
 *   <li>Define a method taking a single argument, the {@link Event} it wants to subscribe to.
 *   <li>Mark that method with a {@link EventListener} annotation.
 *   <li>Pass the listener class instance to {@link #registerListener(Object)}.
 * </ol>
 * <p>
 * Note: event execution is always a blocking operation, <strong>NEVER</strong> should an event listener receive event
 * notifications simultaneously. By default, no event listener will be called when any event publish is ongoing. Other
 * implementations are free to change this behaviour as long as there are no calls to the same event listener
 * simultaneously.
 *
 * @see EventListener
 * @see RegisteredEventListener
 * @since 4.0
 */
public interface EventManager {

  /**
   * Unregisters all listeners in classes which were loaded by the given class loader.
   *
   * @param classLoader the loader based onm which to unregister listeners.
   * @return the same event manager as used to call the method, for chaining.
   * @throws NullPointerException if class loader is null.
   */
  @NonNull EventManager unregisterListeners(@NonNull ClassLoader classLoader);

  /**
   * Unregisters all listeners which were registered in each of the given listener classes.
   *
   * @param listeners the classes to unregister all listeners of.
   * @return the same event manager as used to call the method, for chaining.
   * @throws NullPointerException if any listener instance is null.
   */
  @NonNull EventManager unregisterListener(Object @NonNull ... listeners);

  /**
   * Calls the given event to the * channel, triggering all event listeners which are listening to it.
   * <p>
   * This method call is equivalent to {@code callEvent("*", event)}.
   *
   * @param event the event to call.
   * @param <T>   the type of the event.
   * @return the same event as used to call the method, after processing.
   * @throws NullPointerException   if the given event is null.
   * @throws EventListenerException if any listener threw an exception while processing the event.
   */
  default @NonNull <T extends Event> T callEvent(@NonNull T event) {
    return this.callEvent("*", event);
  }

  /**
   * Calls the given event to the given channel, only triggering the event listeners which are specifically listening to
   * the given channel unless the channel is *.
   *
   * @param channel the specific channel to call the listeners on.
   * @param event   the event to call.
   * @param <T>     the type of the event.
   * @return the same event as used to call the method, after processing.
   * @throws NullPointerException   if the given channel or event is null.
   * @throws EventListenerException if any listener threw an exception while processing the event.
   */
  @NonNull <T extends Event> T callEvent(@NonNull String channel, @NonNull T event);

  /**
   * Registers all methods in the given listener class which are annotated with {@link EventListener} and are taking
   * only one argument with a subtype of {@link Event}. The instance the constructed event listeners are bound to are
   * created by requesting it from the default external injection layer.
   * <p>
   * This method accepts public, protected, default (package) access, and private methods but will not include inherited
   * methods at all.
   * <p>
   * All methods which are not annotated with {@link EventListener}, are static and are not taking one or more arguments
   * are silently ignored.
   *
   * @param listenerClass the class to create an instance of and register all listeners in.
   * @return the same event manager as used to call the method, for chaining.
   * @throws NullPointerException     if the given listener class is null.
   * @throws IllegalArgumentException if an event listener target doesn't take an event as it's first argument.
   */
  @NonNull EventManager registerListener(@NonNull Class<?> listenerClass);

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
   * @param listener the instance of the listener to register the methods in.
   * @return the same event manager as used to call the method, for chaining.
   * @throws NullPointerException     if the given listener is null.
   * @throws IllegalArgumentException if an event listener target doesn't take an event as it's first argument.
   */
  @NonNull EventManager registerListener(@NonNull Object listener);

  /**
   * Registers all listeners which are in the given listener classes individually to this event manager.
   *
   * @param listeners the listeners to register.
   * @return the same event manager as used to call the method, for chaining.
   * @throws NullPointerException     if one of the given listeners is null.
   * @throws IllegalArgumentException if an event listener target doesn't take an event as it's only argument.
   * @see #registerListener(Object)
   */
  default @NonNull EventManager registerListeners(Object @NonNull ... listeners) {
    for (var listener : listeners) {
      this.registerListener(listener);
    }

    return this;
  }
}
