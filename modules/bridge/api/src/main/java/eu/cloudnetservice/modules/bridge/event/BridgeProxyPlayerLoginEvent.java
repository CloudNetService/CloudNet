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
import lombok.NonNull;

/**
 * Called after the cloud player connected to the proxy <strong>AND</strong> connected successfully to a downstream
 * server. This event is called both on all nodes in the cluster and all services running the bridge.
 *
 * @since 4.0
 */
public final class BridgeProxyPlayerLoginEvent extends Event {

  private final CloudPlayer cloudPlayer;

  /**
   * Constructs a new proxy login event with the given cloud player.
   *
   * @param cloudPlayer the cloud player that connected.
   * @throws NullPointerException if the given player is null.
   */
  public BridgeProxyPlayerLoginEvent(@NonNull CloudPlayer cloudPlayer) {
    this.cloudPlayer = cloudPlayer;
  }

  /**
   * Gets the cloud player that connected to a proxy and a service.
   *
   * @return the connected cloud player.
   */
  public @NonNull CloudPlayer cloudPlayer() {
    return this.cloudPlayer;
  }
}
