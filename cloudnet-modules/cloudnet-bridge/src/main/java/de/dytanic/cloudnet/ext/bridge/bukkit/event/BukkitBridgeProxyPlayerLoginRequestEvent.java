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

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * {@inheritDoc}
 */
public final class BukkitBridgeProxyPlayerLoginRequestEvent extends BukkitBridgeEvent {

  private static final HandlerList handlerList = new HandlerList();
  private final NetworkConnectionInfo networkConnectionInfo;

  public BukkitBridgeProxyPlayerLoginRequestEvent(NetworkConnectionInfo networkConnectionInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
  }

  public static HandlerList getHandlerList() {
    return BukkitBridgeProxyPlayerLoginRequestEvent.handlerList;
  }

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }
}
