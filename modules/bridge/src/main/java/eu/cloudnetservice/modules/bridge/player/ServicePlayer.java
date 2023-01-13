/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.document.property.JsonDocPropertyHolder;
import java.util.UUID;
import lombok.NonNull;

/**
 * The service player represents a player that is currently connected to a cloudnet service.
 *
 * @since 4.0
 */
public class ServicePlayer extends JsonDocPropertyHolder implements Comparable<ServicePlayer>, Nameable {

  /**
   * Creates a new service player and appends the name and unique id to the document of the player.
   *
   * @param uniqueId the unique id of the service player.
   * @param name     the name of service player.
   * @throws NullPointerException if the unique id or name is null.
   */
  public ServicePlayer(@NonNull UUID uniqueId, @NonNull String name) {
    super(JsonDocument.newDocument().append("uniqueId", uniqueId).append("name", name));
  }

  /**
   * Creates a new service player with the given properties. The properties have to contain the {@code uniqueId} and
   * {@code name} of the player.
   *
   * @param properties the properties for the service player.
   * @throws NullPointerException if the given properties are null.
   */
  public ServicePlayer(@NonNull JsonDocument properties) {
    super(properties);
  }

  /**
   * Gets the unique id of the service player from the properties of the player.
   *
   * @return the unique id of the player.
   */
  public @NonNull UUID uniqueId() {
    return this.properties.get("uniqueId", UUID.class);
  }

  /**
   * Gets the name of the service player from the properties of the player.
   *
   * @return the name of the player.
   */
  @Override
  public @NonNull String name() {
    return this.properties.getString("name");
  }

  @Override
  public int compareTo(@NonNull ServicePlayer o) {
    return this.name().compareTo(o.name());
  }
}
