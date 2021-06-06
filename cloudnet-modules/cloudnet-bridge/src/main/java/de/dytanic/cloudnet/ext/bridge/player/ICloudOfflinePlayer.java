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

package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.document.gson.IJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the data of an offlinePlayer instance that is saved on the database on CloudNet The player does not need
 * to be online for this item to be provisioned
 */
public interface ICloudOfflinePlayer extends INameable, IJsonDocPropertyable, SerializableObject {

  /**
   * Return the unique identifier of a player from the Minecraft Java or Bedrock Edition
   */
  @NotNull
  UUID getUniqueId();

  /**
   * Sets the players name
   *
   * @param name the name, that should be changed from this object
   */
  void setName(@NotNull String name);

  /**
   * Returns the XBoxId from the offlinePlayer if the player has to be connect from the Minecraft Bedrock Edition
   */
  String getXBoxId();

  long getFirstLoginTimeMillis();

  void setFirstLoginTimeMillis(long firstLoginTimeMillis);

  long getLastLoginTimeMillis();

  void setLastLoginTimeMillis(long lastLoginTimeMillis);

  NetworkConnectionInfo getLastNetworkConnectionInfo();

  void setLastNetworkConnectionInfo(@NotNull NetworkConnectionInfo lastNetworkConnectionInfo);

  JsonDocument getProperties();

  void setProperties(@NotNull JsonDocument properties);
}
