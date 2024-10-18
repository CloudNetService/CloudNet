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

/**
 * This class represents an event which is cancelable. All events which can be prevented from execution should implement
 * this interface. If an event is forced to execute, for example a post-processing event which has just notification
 * purposes, should not implement this interface nor expose any method to prevent the execution of the event in any
 * other way.
 *
 * @since 4.0
 */
public interface Cancelable {

  /**
   * Gets the cancellation state of the event. A cancelled event will stop the execution of the representing change in
   * the system, but will be passed to all event listeners nevertheless.
   *
   * @return true if this is cancelled, false otherwise.
   */
  boolean cancelled();

  /**
   * Marks this event as cancelled and stops the execution of the representing change in the system. The event will be
   * passed to other event listeners down the line anyway.
   *
   * @param value true if this event should get marked as cancelled, false otherwise.
   */
  void cancelled(boolean value);
}
