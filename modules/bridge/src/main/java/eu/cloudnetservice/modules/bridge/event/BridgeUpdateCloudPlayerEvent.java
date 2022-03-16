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
import lombok.NonNull;

/**
 * Called after a cloud player was updated in both the database and all caches. This event is called both on all nodes
 * in the cluster and all services running the bridge.
 */
public final class BridgeUpdateCloudPlayerEvent extends DriverEvent {

  private final CloudPlayer cloudPlayer;

  /**
   * Constructs a new cloud player update event with the given cloud player.
   *
   * @param cloudPlayer the cloud player that was updated.
   * @throws NullPointerException if the given cloud player is null.
   */
  public BridgeUpdateCloudPlayerEvent(@NonNull CloudPlayer cloudPlayer) {
    this.cloudPlayer = cloudPlayer;
  }

  /**
   * Gets the cloud player that was updated.
   *
   * @return the updated cloud player.
   */
  public @NonNull CloudPlayer cloudPlayer() {
    return this.cloudPlayer;
  }
}
