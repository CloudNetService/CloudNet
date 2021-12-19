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
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public class CloudPlayer extends CloudOfflinePlayer {

  protected NetworkServiceInfo loginService;
  protected NetworkServiceInfo connectedService;

  protected NetworkPlayerProxyInfo networkPlayerProxyInfo;
  protected NetworkPlayerServerInfo networkPlayerServerInfo;

  protected JsonDocument onlineProperties;

  public CloudPlayer(
    @NonNull NetworkServiceInfo loginService,
    @NonNull NetworkServiceInfo connectedService,
    @NonNull NetworkPlayerProxyInfo networkPlayerProxyInfo,
    @Nullable NetworkPlayerServerInfo networkPlayerServerInfo,
    @NonNull JsonDocument onlineProperties,
    long firstLoginTimeMillis,
    long lastLoginTimeMillis,
    @NonNull String name,
    @NonNull NetworkPlayerProxyInfo lastNetworkPlayerProxyInfo,
    @NonNull JsonDocument properties
  ) {
    super(firstLoginTimeMillis, lastLoginTimeMillis, name, lastNetworkPlayerProxyInfo);
    this.loginService = loginService;
    this.connectedService = connectedService;
    this.networkPlayerProxyInfo = networkPlayerProxyInfo;
    this.networkPlayerServerInfo = networkPlayerServerInfo;
    this.onlineProperties = onlineProperties;
    this.properties = properties;
  }

  public NetworkServiceInfo loginService() {
    return this.loginService;
  }

  public void loginService(NetworkServiceInfo loginService) {
    this.loginService = loginService;
  }

  public NetworkServiceInfo connectedService() {
    return this.connectedService;
  }

  public void connectedService(NetworkServiceInfo connectedService) {
    this.connectedService = connectedService;
  }

  public NetworkPlayerProxyInfo networkPlayerProxyInfo() {
    return this.networkPlayerProxyInfo;
  }

  public void networkPlayerProxyInfo(NetworkPlayerProxyInfo networkPlayerProxyInfo) {
    this.networkPlayerProxyInfo = networkPlayerProxyInfo;
  }

  public NetworkPlayerServerInfo networkPlayerServerInfo() {
    return this.networkPlayerServerInfo;
  }

  public void networkPlayerServerInfo(NetworkPlayerServerInfo networkPlayerServerInfo) {
    this.networkPlayerServerInfo = networkPlayerServerInfo;
  }

  public JsonDocument onlineProperties() {
    return this.onlineProperties;
  }

  public PlayerExecutor playerExecutor() {
    return CloudNetDriver.instance().servicesRegistry()
      .firstService(IPlayerManager.class)
      .playerExecutor(this.uniqueId());
  }
}
