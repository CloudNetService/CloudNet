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

package eu.cloudnetservice.driver.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to mark a method in a class as a method listening for an event. An event method should only take one
 * argument which is the event the method is listening to. The argument type must extend from {@link Event} and must be
 * the event to listen to, not a super class of an event. More specific filtering can be done by providing a specific
 * channel this event listener listens to. The event channel can be specified by when calling the event being listened
 * to.
 * <p>
 * All listeners (or in other words, all method annotated with this annotation) specified in a class will get registered
 * when calling {@link EventManager#registerListener(Object)}.
 *
 * @see EventManager#registerListener(Object)
 * @see EventManager#callEvent(String, Event)
 * @since 4.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {

  /**
   * The channel this event is listening to. Defaults to * meaning that the listener listens to all channels.
   *
   * @return the channel this listener listens to.
   */
  String channel() default "*";

  /**
   * Defines the priority of this event. A lower priority means that the event is called earlier in the chain. A high
   * priority is useful if the listener should set the final result of the event. Defaults to NORMAL.
   * <p>
   * First priority to the last executed:
   * <ol>
   *   <li>LOWEST
   *   <li>LOW
   *   <li>NORMAL
   *   <li>HIGH
   *   <li>HIGHEST
   * </ol>
   *
   * @return the priority of the listener.
   */
  EventPriority priority() default EventPriority.NORMAL;
}
