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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public interface ICloudPlayer extends ICloudOfflinePlayer, SerializableObject {

  /**
   * Gets the {@link NetworkServiceInfo} for the service the players used to connect. In most cases this is a proxy.
   *
   * @return the {@link NetworkServiceInfo} the player used to connect
   */
  NetworkServiceInfo getLoginService();

  /**
   * Sets the given loginService as the service the player used to connect.
   *
   * @param loginService the loginService used
   */
  void setLoginService(NetworkServiceInfo loginService);

  /**
   * Gets the current {@link NetworkServiceInfo} to which the player is connected to (usually not a proxy).
   *
   * @return the connected service information
   */
  NetworkServiceInfo getConnectedService();

  /**
   * Sets the given connectedService as the service the player is connected to
   *
   * @param connectedService the connectedService the player is connected to
   */
  void setConnectedService(NetworkServiceInfo connectedService);

  /**
   * @return the current {@link NetworkConnectionInfo} for this player
   */
  NetworkConnectionInfo getNetworkConnectionInfo();

  /**
   * Sets the current {@link NetworkConnectionInfo} for this player
   *
   * @param networkConnectionInfo the connectionInfo of this player
   */
  void setNetworkConnectionInfo(NetworkConnectionInfo networkConnectionInfo);

  /**
   * Gets the last {@link NetworkPlayerServerInfo} with player specific information like the health and the location in
   * the world
   *
   * @return the last {@link NetworkPlayerServerInfo} of this player
   */
  NetworkPlayerServerInfo getNetworkPlayerServerInfo();

  /**
   * Sets the {@link NetworkPlayerServerInfo} of this player to the given one
   *
   * @param networkPlayerServerInfo the new playerServerInfo
   */
  void setNetworkPlayerServerInfo(NetworkPlayerServerInfo networkPlayerServerInfo);

  /**
   * Gets the properties of the player that are only present while the player is online, if the player disconnects the
   * properties are gone
   *
   * @return the players online properties
   */
  @NotNull
  JsonDocument getOnlineProperties();

  /**
   * Creates a {@link PlayerExecutor} for the player
   * <p>
   * See {@link IPlayerManager#getPlayerExecutor(UUID)}
   *
   * @return a new {@link PlayerExecutor} for this player
   */
  PlayerExecutor getPlayerExecutor();

}
