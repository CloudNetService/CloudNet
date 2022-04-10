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

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.event.invoker.ListenerInvoker;
import lombok.NonNull;

record DefaultRegisteredEventListener(
  @NonNull Object instance,
  @NonNull String methodName,
  @NonNull Class<?> eventClass,
  @NonNull EventListener eventListener,
  @NonNull ListenerInvoker listenerInvoker
) implements RegisteredEventListener {

  private static final Logger LOGGER = LogManager.logger(DefaultRegisteredEventListener.class);

  @Override
  public void fireEvent(@NonNull Event event) {
    LOGGER.fine(
      "Calling event %s on listener %s",
      null,
      event.getClass().getName(),
      this.instance().getClass().getName());

    try {
      this.listenerInvoker.invoke(this.instance, event);
    } catch (Exception exception) {
      throw new EventListenerException(String.format(
        "Error while invoking event listener %s in class %s",
        this.methodName,
        this.instance.getClass().getName()), exception);
    }
  }

  @Override
  public @NonNull String channel() {
    return this.eventListener.channel();
  }

  @Override
  public @NonNull EventPriority priority() {
    return this.eventListener.priority();
  }
}
