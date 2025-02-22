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
 * Represents an event which can be fired. This class is just a marker class to indicate that a class is an event.
 * Events can be fired by using {@link EventManager#callEvent(Event)} and listened to by using a method with only one
 * parameter (the event to listen to) which is annotated with {@code @EventListener}.
 *
 * @see EventManager#callEvent(Event)
 * @see EventListener
 * @see Cancelable
 * @since 4.0
 */
public abstract class Event {

}
