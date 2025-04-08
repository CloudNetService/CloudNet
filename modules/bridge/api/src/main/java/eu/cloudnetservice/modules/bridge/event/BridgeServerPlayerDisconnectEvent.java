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

package eu.cloudnetservice.modules.bridge.event;

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import lombok.NonNull;

/**
 * Called when a cloud player disconnects from any downstream service but before the player connected to a new service.
 * This event is called both on all nodes in the cluster and all services running the bridge.
 *
 * @since 4.0
 */
public final class BridgeServerPlayerDisconnectEvent extends Event {

  private final CloudPlayer cloudPlayer;
  private final NetworkServiceInfo serviceInfo;

  /**
   * Constructs a new player server disconnect event for the given cloud player and the service the player disconnected
   * from.
   *
   * @param cloudPlayer the player that disconnected.
   * @param serviceInfo the service the player disconnected from.
   * @throws NullPointerException if the given player or service info is null.
   */
  public BridgeServerPlayerDisconnectEvent(@NonNull CloudPlayer cloudPlayer, @NonNull NetworkServiceInfo serviceInfo) {
    this.cloudPlayer = cloudPlayer;
    this.serviceInfo = serviceInfo;
  }

  /**
   * Gets the player that disconnected from a downstream service.
   *
   * @return the player that disconnected.
   */
  public @NonNull CloudPlayer cloudPlayer() {
    return this.cloudPlayer;
  }

  /**
   * Gets the network service info of the service the player disconnected from.
   *
   * @return the network service info of the service.
   */
  public @NonNull NetworkServiceInfo service() {
    return this.serviceInfo;
  }
}
