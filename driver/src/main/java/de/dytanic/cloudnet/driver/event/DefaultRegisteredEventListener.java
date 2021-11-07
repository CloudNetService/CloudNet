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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
final class DefaultRegisteredEventListener implements IRegisteredEventListener {

  private static final Logger LOGGER = LogManager.getLogger(DefaultRegisteredEventListener.class);

  private final Object instance;
  private final String methodName;
  private final Class<?> eventClass;
  private final EventListener eventListener;
  private final ListenerInvoker listenerInvoker;

  public DefaultRegisteredEventListener(
    @NotNull Object instance,
    @NotNull String methodName,
    @NotNull Class<?> eventClass,
    @NotNull EventListener eventListener,
    @NotNull ListenerInvoker listenerInvoker
  ) {
    this.instance = instance;
    this.methodName = methodName;
    this.eventClass = eventClass;
    this.eventListener = eventListener;
    this.listenerInvoker = listenerInvoker;
  }

  @Override
  public void fireEvent(@NotNull Event event) {
    if (event.isShowDebug()) {
      LOGGER.fine(String.format(
        "Calling event %s on listener %s",
        event.getClass().getName(),
        this.getInstance().getClass().getName()));
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
  public @NotNull EventListener getEventListener() {
    return this.eventListener;
  }

  @Override
  public @NotNull EventPriority getPriority() {
    return this.eventListener.priority();
  }

  @Override
  public @NotNull String getChannel() {
    return this.eventListener.channel();
  }

  @Override
  public @NotNull Object getInstance() {
    return this.instance;
  }

  @Override
  public @NotNull Class<?> getEventClass() {
    return this.eventClass;
  }
}
