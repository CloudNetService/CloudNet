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
import org.jetbrains.annotations.Nullable;

/**
 * Called after the cloud player switched its downstream service to another one. At this point
 * {@link CloudPlayer#connectedService()} returns the new service. This event is called both on all nodes in the cluster
 * and all services running the bridge.
 *
 * @since 4.0
 */
public final class BridgeProxyPlayerServerSwitchEvent extends Event {

  private final CloudPlayer cloudPlayer;
  private final NetworkServiceInfo previous;

  /**
   * Constructs a new server switch event with the given cloud player and the previous service.
   *
   * @param player   the cloud player that switched the server.
   * @param previous the network service the player was connected to previously.
   * @throws NullPointerException if the given player is null.
   */
  public BridgeProxyPlayerServerSwitchEvent(
    @NonNull CloudPlayer player,
    @Nullable NetworkServiceInfo previous
  ) {
    this.cloudPlayer = player;
    this.previous = previous;
  }

  /**
   * Gets the cloud player that switched servers.
   *
   * @return the player that switched servers.
   */
  public @NonNull CloudPlayer cloudPlayer() {
    return this.cloudPlayer;
  }

  /**
   * Gets the network service info that the cloud player is now connected to.
   *
   * @return the target network service info
   */
  public @NonNull NetworkServiceInfo target() {
    //noinspection ConstantConditions
    return this.cloudPlayer.connectedService();
  }

  /**
   * Gets the network service info that the cloud player was connected to previously.
   *
   * @return the previous network service info, null if the player was not connected to any service previously.
   */
  public @Nullable NetworkServiceInfo previous() {
    return this.previous;
  }
}
