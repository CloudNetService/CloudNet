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

import java.util.Comparator;
import lombok.NonNull;

/**
 * An event priority during execution set by the corresponding {@link EventListener} annotation for the listener.
 *
 * @author Pasqual Koschmieder (derklaro@cloudnetservice.eu)
 * @see EventListener
 * @see RegisteredEventListener
 * @since 4.0
 */
public enum EventPriority implements Comparator<EventPriority> {

  /**
   * The listener has the highest priority and should always decide about the outcome of the event.
   */
  HIGHEST(128),
  /**
   * The listener has a higher importance than a normal listener but isn't required to decide about the outcome.
   */
  HIGH(64),
  /**
   * The listener call is neither important nor unimportant and should run normally. This is the default behaviour.
   */
  NORMAL(32),
  /**
   * The listener has no high importance for the outcome of the event call.
   */
  LOW(16),
  /**
   * The listener has no importance for the event outcome.
   */
  LOWEST(8);

  private final int value;

  /**
   * Constructs a new event priority.
   *
   * @param value the priority in int form, decides about the order of the priority enum.
   */
  EventPriority(int value) {
    this.value = value;
  }

  /**
   * Get the priority value in form of an int for comparison reasons.
   *
   * @return the priority value in form of an int for comparison reasons.
   */
  public int value() {
    return this.value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compare(@NonNull EventPriority o1, @NonNull EventPriority o2) {
    return Integer.compare(o1.value, o2.value);
  }
}
