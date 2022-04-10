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

package eu.cloudnetservice.modules.bridge.event;

import eu.cloudnetservice.cloudnet.driver.event.events.DriverEvent;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import lombok.NonNull;

/**
 * Called when a cloud player connects to a downstream service. This event is called both on all nodes in the cluster
 * and all services running the bridge.
 *
 * @since 4.0
 */
public final class BridgeServerPlayerLoginEvent extends DriverEvent {

  private final CloudPlayer cloudPlayer;
  private final NetworkPlayerServerInfo serverInfo;

  /**
   * Constructs a new player server login event for the given cloud player and the service the player connected to.
   *
   * @param cloudPlayer the player that disconnected.
   * @param serverInfo  the service the player connected to.
   * @throws NullPointerException if the given player or service info is null.
   */
  public BridgeServerPlayerLoginEvent(@NonNull CloudPlayer cloudPlayer, @NonNull NetworkPlayerServerInfo serverInfo) {
    this.cloudPlayer = cloudPlayer;
    this.serverInfo = serverInfo;
  }

  /**
   * Gets the player that connected to the given downstream service.
   *
   * @return the player that connected to downstream service.
   */
  public @NonNull CloudPlayer cloudPlayer() {
    return this.cloudPlayer;
  }

  /**
   * Gets the network service info of the service the player connected tp.
   *
   * @return the network service info of the service.
   */
  public @NonNull NetworkPlayerServerInfo service() {
    return this.serverInfo;
  }
}
