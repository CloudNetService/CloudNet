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

package eu.cloudnetservice.driver.event.invoker;

import eu.cloudnetservice.driver.event.Event;
import lombok.NonNull;

/**
 * Represents an invoker for a listener method. An invoker can be implemented in many forms, by default an invoker gets
 * dynamically generated in the runtime by using code generation. Other implementations (like reflection based method
 * invocation) are possible too.
 *
 * @see ListenerInvokerGenerator
 * @since 4.0
 */
@FunctionalInterface
public interface ListenerInvoker {

  /**
   * Invokes the target event listener method on the given instance with the given event. The event should always be a
   * subtype of the event provided during listener registration in the listener method.
   * <p>
   * Uncaught exceptions will be passed through to the caller.
   * <p>
   * Event execution is not concurrent as per the event manager contract, therefore there is no need for locking before
   * event execution.
   *
   * @param listenerInstance the instance of the listener to call the event on.
   * @param event            the event causing the invocation request.
   * @throws NullPointerException if the listener instance or event is null.
   */
  void invoke(@NonNull Object listenerInstance, @NonNull Event event);
}
