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

import lombok.NonNull;

/**
 * Represents a listener which is registered in an event manager and is ready to accept an event. Event execution is not
 * concurrent as per the event manager contract, therefore there is no need for locking before event execution.
 *
 * @see EventManager
 * @since 4.0
 */
public interface RegisteredEventListener extends Comparable<RegisteredEventListener> {

  /**
   * Fires the event by invoking the underlying method with the given event. The event type is ensured to only be the
   * same type the listener defined in the method. Event execution is not concurrent as per the event manager contract,
   * therefore there is no need for locking before event execution.
   *
   * @param event the event to fire.
   * @throws NullPointerException   if the given event is null.
   * @throws EventListenerException if the underlying listeners throws an exception while handling the event.
   */
  void fireEvent(@NonNull Event event);

  /**
   * Get the annotation used on the original listener method to identify it as a listener method.
   *
   * @return the annotation used on the original listener method.
   */
  @NonNull EventListener eventListener();

  /**
   * Get the priority defined in the @EventListener annotation on the original listener method.
   *
   * @return the priority of the underlying event listener.
   */
  @NonNull EventPriority priority();

  /**
   * Get the channel this event is listening to. Defaults to * meaning that the listener listens to all channels.
   *
   * @return the channel this event is listening to.
   */
  @NonNull String channel();

  /**
   * Get the instance of the listener class used to register all listeners in it.
   *
   * @return the instance of the listener class.
   */
  @NonNull Object instance();

  /**
   * Get the class type of the event the underlying listener is listening to.
   *
   * @return the class type of the event this listener listens to.
   */
  @NonNull Class<?> eventClass();

  /**
   * {@inheritDoc}
   */
  @Override
  default int compareTo(@NonNull RegisteredEventListener other) {
    return other.priority().compareTo(this.priority());
  }
}
