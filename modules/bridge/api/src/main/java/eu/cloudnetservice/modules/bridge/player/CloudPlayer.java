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

import eu.cloudnetservice.driver.document.Document;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * The online player is a player who is currently connected to one of the proxies. The player is in the cache of each
 * node as long as he is online.
 * <p>
 * Obtain an online player using {@link PlayerManager#onlinePlayer(UUID)}.
 * <p>
 * If you want to interact with a player use the
 * {@link eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor} instead.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode(callSuper = true)
public class CloudPlayer extends CloudOfflinePlayer {

  protected final NetworkPlayerProxyInfo networkPlayerProxyInfo;

  protected NetworkServiceInfo loginService;
  protected NetworkServiceInfo connectedService;
  protected NetworkPlayerServerInfo networkPlayerServerInfo;

  protected Document onlineProperties;

  /**
   * Constructs a new online cloud player.
   *
   * @param loginService            the service that was used to connect to the network.
   * @param connectedService        the downstream service the player is connected to.
   * @param networkPlayerProxyInfo  the player proxy info for the player.
   * @param networkPlayerServerInfo the player server info for the player - null if there is no downstream service.
   * @param onlineProperties        all properties of the player that are only present when online.
   * @param name                    the name of the cloud player.
   * @param firstLoginTimeMillis    the time-stamp the player connected the first time.
   * @param lastLoginTimeMillis     the time-stamp the player connected the last time.
   * @param properties              the properties of the offline player that are persisted in the database.
   * @throws NullPointerException if the given login service, connected service, proxy info, online properties, name,
   *                              last proxy info or properties is null.
   */
  public CloudPlayer(
    @NonNull NetworkPlayerProxyInfo networkPlayerProxyInfo,
    @NonNull NetworkServiceInfo loginService,
    @NonNull NetworkServiceInfo connectedService,
    @Nullable NetworkPlayerServerInfo networkPlayerServerInfo,
    @NonNull Document onlineProperties,
    @NonNull String name,
    long firstLoginTimeMillis,
    long lastLoginTimeMillis,
    @NonNull NetworkPlayerProxyInfo lastNetworkPlayerProxyInfo,
    @NonNull Document properties
  ) {
    super(name, firstLoginTimeMillis, lastLoginTimeMillis, lastNetworkPlayerProxyInfo, properties);

    this.loginService = loginService;
    this.connectedService = connectedService;
    this.networkPlayerProxyInfo = networkPlayerProxyInfo;
    this.networkPlayerServerInfo = networkPlayerServerInfo;
    this.onlineProperties = onlineProperties;
  }

  /**
   * Gets the service info of the proxy the player used to connect to the network.
   *
   * @return the service info of the proxy.
   */
  public @NonNull NetworkServiceInfo loginService() {
    return this.loginService;
  }

  /**
   * Sets the login service info that indicates the proxy the player used to connect to the network.
   *
   * @param loginService the login service info to set.
   * @throws NullPointerException if the given login service is null.
   */
  public void loginService(@NonNull NetworkServiceInfo loginService) {
    this.loginService = loginService;
  }

  /**
   * Gets the downstream service the player is connected to. Initially this is the proxy service until the player is
   * connected to the fallback. Might be null if the player is not connected to a known downstream service.
   *
   * @return the downstream service the player is connected to, null if the player is not connected to one.
   */
  public @Nullable NetworkServiceInfo connectedService() {
    return this.connectedService;
  }

  /**
   * Sets the downstream service the player is currently connected to.
   *
   * @param connectedService the downstream service the player is connected to.
   */
  public void connectedService(@Nullable NetworkServiceInfo connectedService) {
    this.connectedService = connectedService;
  }

  /**
   * Gets the network player proxy info for the current connection between the player and the proxy.
   *
   * @return the network player proxy info.
   */
  public @NonNull NetworkPlayerProxyInfo networkPlayerProxyInfo() {
    return this.networkPlayerProxyInfo;
  }

  /**
   * Gets the network player server info for the player and the downstream service the player is connected to.
   *
   * @return the network player server info of the downstream the player is connected to, null if the player is not
   * connected to any downstream service.
   */
  public @Nullable NetworkPlayerServerInfo networkPlayerServerInfo() {
    return this.networkPlayerServerInfo;
  }

  /**
   * Sets the network player info to the given server info. Indicating a new downstream service the player is connected
   * to.
   *
   * @param networkPlayerServerInfo the new player server info.
   * @throws NullPointerException if the given server info is null.
   */
  public void networkPlayerServerInfo(@NonNull NetworkPlayerServerInfo networkPlayerServerInfo) {
    this.networkPlayerServerInfo = networkPlayerServerInfo;
    this.connectedService = networkPlayerServerInfo.networkService();
  }

  /**
   * Gets all properties of the player that are only present when online. These properties are not persisted in the
   * database and are lost after the player disconnects.
   *
   * @return the online properties of the player.
   */
  public @NonNull Document onlineProperties() {
    return this.onlineProperties;
  }

  /**
   * Sets the online properties of this cloud player.
   *
   * @param onlineProperties the properties to set.
   * @throws NullPointerException if the given properties are null.
   */
  public void onlineProperties(@NonNull Document onlineProperties) {
    this.onlineProperties = onlineProperties.immutableCopy();
  }
}
