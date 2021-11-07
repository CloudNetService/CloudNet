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

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public class CloudPlayer extends CloudOfflinePlayer {

  public static final Type COLLECTION_TYPE = TypeToken.getParameterized(Collection.class, CloudPlayer.class).getType();

  protected NetworkServiceInfo loginService;
  protected NetworkServiceInfo connectedService;

  protected NetworkPlayerProxyInfo networkPlayerProxyInfo;
  protected NetworkPlayerServerInfo networkPlayerServerInfo;

  protected JsonDocument onlineProperties;

  public CloudPlayer(
    @NotNull NetworkServiceInfo loginService,
    @NotNull NetworkServiceInfo connectedService,
    @NotNull NetworkPlayerProxyInfo networkPlayerProxyInfo,
    @Nullable NetworkPlayerServerInfo networkPlayerServerInfo,
    @NotNull JsonDocument onlineProperties,
    long firstLoginTimeMillis,
    long lastLoginTimeMillis,
    @NotNull NetworkPlayerProxyInfo lastNetworkPlayerProxyInfo,
    @NotNull JsonDocument properties
  ) {
    super(firstLoginTimeMillis, lastLoginTimeMillis, lastNetworkPlayerProxyInfo);
    this.loginService = loginService;
    this.connectedService = connectedService;
    this.networkPlayerProxyInfo = networkPlayerProxyInfo;
    this.networkPlayerServerInfo = networkPlayerServerInfo;
    this.onlineProperties = onlineProperties;
    this.properties = properties;
  }

  public NetworkServiceInfo getLoginService() {
    return this.loginService;
  }

  public void setLoginService(NetworkServiceInfo loginService) {
    this.loginService = loginService;
  }

  public NetworkServiceInfo getConnectedService() {
    return this.connectedService;
  }

  public void setConnectedService(NetworkServiceInfo connectedService) {
    this.connectedService = connectedService;
  }

  public NetworkPlayerProxyInfo getNetworkPlayerProxyInfo() {
    return this.networkPlayerProxyInfo;
  }

  public void setNetworkPlayerProxyInfo(NetworkPlayerProxyInfo networkPlayerProxyInfo) {
    this.networkPlayerProxyInfo = networkPlayerProxyInfo;
  }

  public NetworkPlayerServerInfo getNetworkPlayerServerInfo() {
    return this.networkPlayerServerInfo;
  }

  public void setNetworkPlayerServerInfo(NetworkPlayerServerInfo networkPlayerServerInfo) {
    this.networkPlayerServerInfo = networkPlayerServerInfo;
  }

  public JsonDocument getOnlineProperties() {
    return this.onlineProperties;
  }

  public PlayerExecutor getPlayerExecutor() {
    return CloudNetDriver.getInstance().getServicesRegistry()
      .getFirstService(IPlayerManager.class)
      .getPlayerExecutor(this.getUniqueId());
  }
}
