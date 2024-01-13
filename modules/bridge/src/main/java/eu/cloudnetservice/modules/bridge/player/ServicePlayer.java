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

package eu.cloudnetservice.modules.bridge.player;

import java.util.UUID;
import lombok.NonNull;

/**
 * The service player represents a player that is currently connected to a cloudnet service.
 *
 * @param uniqueId the unique id of the player.
 * @param name     the name of the player.
 * @since 4.0
 */
public record ServicePlayer(@NonNull UUID uniqueId, @NonNull String name) implements Comparable<ServicePlayer> {

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(@NonNull ServicePlayer o) {
    return this.name().compareTo(o.name());
  }
}
