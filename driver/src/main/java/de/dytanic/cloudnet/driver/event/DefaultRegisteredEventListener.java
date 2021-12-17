/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.event;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.event.invoker.ListenerInvoker;
import org.jetbrains.annotations.NotNull;

record DefaultRegisteredEventListener(
  @NotNull Object instance,
  @NotNull String methodName,
  @NotNull Class<?> eventClass,
  @NotNull EventListener eventListener,
  @NotNull ListenerInvoker listenerInvoker
) implements IRegisteredEventListener {

  private static final Logger LOGGER = LogManager.getLogger(DefaultRegisteredEventListener.class);

  @Override
  public void fireEvent(@NotNull Event event) {
    if (event.showDebug()) {
      LOGGER.fine(String.format(
        "Calling event %s on listener %s",
        event.getClass().getName(), this.instance().getClass().getName()));
    }

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
  public @NotNull String channel() {
    return this.eventListener.channel();
  }

  @Override
  public @NotNull EventPriority priority() {
    return this.eventListener.priority();
  }
}
