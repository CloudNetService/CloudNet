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
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

@ToString
@EqualsAndHashCode(callSuper = false)
public class CloudOfflinePlayer extends JsonDocPropertyHolder implements Cloneable, INameable {

  protected long firstLoginTimeMillis;
  protected long lastLoginTimeMillis;

  protected NetworkPlayerProxyInfo lastNetworkPlayerProxyInfo;

  public CloudOfflinePlayer(long firstLoginTimeMillis, long lastLoginTimeMillis, NetworkPlayerProxyInfo proxyInfo) {
    this.firstLoginTimeMillis = firstLoginTimeMillis;
    this.lastLoginTimeMillis = lastLoginTimeMillis;
    this.lastNetworkPlayerProxyInfo = proxyInfo;
  }

  public static @NotNull CloudOfflinePlayer offlineCopy(@NotNull CloudPlayer onlineVariant) {
    return new CloudOfflinePlayer(
      onlineVariant.getFirstLoginTimeMillis(),
      onlineVariant.getLastLoginTimeMillis(),
      onlineVariant.getNetworkPlayerProxyInfo().clone());
  }

  public @NotNull UUID getUniqueId() {
    return this.lastNetworkPlayerProxyInfo.getUniqueId();
  }

  @Override
  public @NotNull String getName() {
    return this.lastNetworkPlayerProxyInfo.getName();
  }

  public @UnknownNullability String getXBoxId() {
    return this.lastNetworkPlayerProxyInfo.getXBoxId();
  }

  public long getFirstLoginTimeMillis() {
    return this.firstLoginTimeMillis;
  }

  public long getLastLoginTimeMillis() {
    return this.lastLoginTimeMillis;
  }

  public void setLastLoginTimeMillis(long lastLoginTimeMillis) {
    this.lastLoginTimeMillis = lastLoginTimeMillis;
  }

  public @NotNull NetworkPlayerProxyInfo getLastNetworkPlayerProxyInfo() {
    return this.lastNetworkPlayerProxyInfo;
  }

  public void setLastNetworkPlayerProxyInfo(@NotNull NetworkPlayerProxyInfo lastNetworkPlayerProxyInfo) {
    this.lastNetworkPlayerProxyInfo = lastNetworkPlayerProxyInfo;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull CloudOfflinePlayer clone() {
    try {
      return (CloudOfflinePlayer) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new RuntimeException();
    }
  }
}
