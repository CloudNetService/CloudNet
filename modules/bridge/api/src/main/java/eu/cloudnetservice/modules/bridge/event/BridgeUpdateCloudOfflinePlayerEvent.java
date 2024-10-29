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
import eu.cloudnetservice.modules.bridge.player.CloudOfflinePlayer;
import lombok.NonNull;

/**
 * Called after a cloud offline player was updated in both the database and all caches. This event is called both on all
 * nodes in the cluster and all services running the bridge.
 *
 * @since 4.0
 */
public final class BridgeUpdateCloudOfflinePlayerEvent extends Event {

  private final CloudOfflinePlayer cloudOfflinePlayer;

  /**
   * Constructs a new cloud offline player update event with the given cloud offline player.
   *
   * @param cloudOfflinePlayer the cloud offline player that was updated.
   * @throws NullPointerException if the given cloud offline player is null.
   */
  public BridgeUpdateCloudOfflinePlayerEvent(@NonNull CloudOfflinePlayer cloudOfflinePlayer) {
    this.cloudOfflinePlayer = cloudOfflinePlayer;
  }

  /**
   * Gets the cloud offline player that was updated.
   *
   * @return the updated cloud offline player.
   */
  public @NonNull CloudOfflinePlayer cloudOfflinePlayer() {
    return this.cloudOfflinePlayer;
  }
}
