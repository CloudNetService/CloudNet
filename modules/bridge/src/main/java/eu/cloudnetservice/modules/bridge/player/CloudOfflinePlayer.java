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

package eu.cloudnetservice.modules.bridge.player;

import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.UnknownNullability;

@ToString
@EqualsAndHashCode(callSuper = false)
public class CloudOfflinePlayer extends JsonDocPropertyHolder implements Cloneable, Nameable {

  protected final String name;

  protected long firstLoginTimeMillis;
  protected long lastLoginTimeMillis;
  protected NetworkPlayerProxyInfo lastNetworkPlayerProxyInfo;

  public CloudOfflinePlayer(
    @NonNull String name,
    long firstLoginTimeMillis,
    long lastLoginTimeMillis,
    @NonNull NetworkPlayerProxyInfo proxyInfo,
    @NonNull JsonDocument properties
  ) {
    super(properties);
    this.name = name;
    this.firstLoginTimeMillis = firstLoginTimeMillis;
    this.lastLoginTimeMillis = lastLoginTimeMillis;
    this.lastNetworkPlayerProxyInfo = proxyInfo;
  }

  public static @NonNull CloudOfflinePlayer offlineCopy(@NonNull CloudPlayer onlineVariant) {
    return new CloudOfflinePlayer(
      onlineVariant.name(),
      onlineVariant.firstLoginTimeMillis(),
      onlineVariant.lastLoginTimeMillis(),
      onlineVariant.networkPlayerProxyInfo().clone(),
      onlineVariant.properties().clone());
  }

  public @NonNull UUID uniqueId() {
    return this.lastNetworkPlayerProxyInfo.uniqueId();
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }

  public @UnknownNullability String xBoxId() {
    return this.lastNetworkPlayerProxyInfo.xBoxId();
  }

  public long firstLoginTimeMillis() {
    return this.firstLoginTimeMillis;
  }

  public long lastLoginTimeMillis() {
    return this.lastLoginTimeMillis;
  }

  public void lastLoginTimeMillis(long lastLoginTimeMillis) {
    this.lastLoginTimeMillis = lastLoginTimeMillis;
  }

  public @NonNull NetworkPlayerProxyInfo lastNetworkPlayerProxyInfo() {
    return this.lastNetworkPlayerProxyInfo;
  }

  public void lastNetworkPlayerProxyInfo(@NonNull NetworkPlayerProxyInfo lastNetworkPlayerProxyInfo) {
    this.lastNetworkPlayerProxyInfo = lastNetworkPlayerProxyInfo;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CloudOfflinePlayer clone() {
    try {
      return (CloudOfflinePlayer) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new RuntimeException();
    }
  }
}
