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

package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.WrappedChannelMessageReceiveEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * {@inheritDoc}
 */
public final class BukkitChannelMessageReceiveEvent extends BukkitCloudNetEvent implements
  WrappedChannelMessageReceiveEvent {

  private static final HandlerList handlerList = new HandlerList();

  private final ChannelMessageReceiveEvent event;

  public BukkitChannelMessageReceiveEvent(ChannelMessageReceiveEvent event) {
    this.event = event;
  }

  public static HandlerList getHandlerList() {
    return BukkitChannelMessageReceiveEvent.handlerList;
  }

  @Override
  public ChannelMessageReceiveEvent getWrapped() {
    return this.event;
  }

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }
}
