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
 * An event priority during execution set by the corresponding {@link EventListener} annotation for the listener.
 *
 * @see EventListener
 * @see RegisteredEventListener
 * @since 4.0
 */
public enum InvocationOrder {

  /**
   * The associated event listener has the highest priority and will be called first. All other event listeners will be
   * called after that one. This should mainly get used for monitoring reasons or to early set the result of an event,
   * allowing other listeners to still modify it.
   */
  FIRST,
  /**
   * The associated event listener gets called earlier than normal listeners.
   */
  EARLY,
  /**
   * The normal order entry each event listener without a specific order assigned to it will use. This order is
   * perfectly aligned between the early listeners and the late listeners, allowing plugins or modules to adjust their
   * outcome over normal registered listeners.
   */
  NORMAL,
  /**
   * The associated event listener is called after all normal and early event listeners and has a more decisive place in
   * the outcome of the called event. This priority should be used if no real need is there to really decide about the
   * outcome of an event, but normal listeners should be overridden nevertheless.
   */
  LATE,
  /**
   * The associated event listener is called last in the event invocation chain. The associated listener always decides
   * about the outcome of an event and overrides all other values set before.
   */
  LAST,
  /**
   * The last invoked listener in the chain. Associated listeners with that priority should <strong>NEVER</strong> make
   * changes to the called event and only monitor the outcome of the event (for example to log the event status).
   */
  MONITOR
}
