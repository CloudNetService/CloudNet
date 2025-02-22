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

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * The offline player represents a player who was either already connected on one of the proxies or was created
 * manually.
 * <p>
 * Obtain an offline player using {@link PlayerManager#offlinePlayer(UUID)}.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public class CloudOfflinePlayer implements DefaultedDocPropertyHolder, Cloneable, Named {

  protected final String name;

  protected long firstLoginTimeMillis;
  protected long lastLoginTimeMillis;

  protected NetworkPlayerProxyInfo lastNetworkPlayerProxyInfo;
  protected Document properties;

  /**
   * Constructs a new cloud offline player.
   *
   * @param name                 the name of the cloud player
   * @param firstLoginTimeMillis the time-stamp the player connected the first time.
   * @param lastLoginTimeMillis  the time-stamp the player connected the last time.
   * @param proxyInfo            the player proxy info given information about the last connection to the proxy.
   * @param properties           properties belonging to the player.
   * @throws NullPointerException if the given name, proxy info or properties is null.
   */
  public CloudOfflinePlayer(
    @NonNull String name,
    long firstLoginTimeMillis,
    long lastLoginTimeMillis,
    @NonNull NetworkPlayerProxyInfo proxyInfo,
    @NonNull Document properties
  ) {
    this.name = name;
    this.firstLoginTimeMillis = firstLoginTimeMillis;
    this.lastLoginTimeMillis = lastLoginTimeMillis;
    this.properties = properties;
    this.lastNetworkPlayerProxyInfo = proxyInfo;
  }

  /**
   * Creates an offline copy from the given cloud online player.
   *
   * @param onlineVariant the online player to create a copy from.
   * @return the new offline player copy.
   * @throws NullPointerException if the given online player is null.
   */
  public static @NonNull CloudOfflinePlayer offlineCopy(@NonNull CloudPlayer onlineVariant) {
    return new CloudOfflinePlayer(
      onlineVariant.name(),
      onlineVariant.firstLoginTimeMillis(),
      onlineVariant.lastLoginTimeMillis(),
      onlineVariant.networkPlayerProxyInfo().clone(),
      onlineVariant.propertyHolder().immutableCopy());
  }

  /**
   * Gets the unique id of this cloud player from the last proxy info.
   *
   * @return the unique id of the player.
   */
  public @NonNull UUID uniqueId() {
    return this.lastNetworkPlayerProxyInfo.uniqueId();
  }

  /**
   * Gets the name of this cloud player.
   *
   * @return the name of the player.
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }

  /**
   * Gets the xbox id of the player, null if the player does not have a xbox id.
   *
   * @return the xbox id of the player, null no xbox id was found.
   */
  public @Nullable String xBoxId() {
    return this.lastNetworkPlayerProxyInfo.xBoxId();
  }

  /**
   * Gets the time-stamp when the player connected for the first time.
   *
   * @return the time-stamp for the fist connection.
   */
  public long firstLoginTimeMillis() {
    return this.firstLoginTimeMillis;
  }

  /**
   * Gets the time-stamp when the player connected last time.
   *
   * @return the time-stamp for the last connection.
   */
  public long lastLoginTimeMillis() {
    return this.lastLoginTimeMillis;
  }

  /**
   * Gets the last known proxy player info for this player.
   *
   * @return the proxy player info for this player.
   */
  public @NonNull NetworkPlayerProxyInfo lastNetworkPlayerProxyInfo() {
    return this.lastNetworkPlayerProxyInfo;
  }

  /**
   * Sets the last known proxy player info of this player.
   *
   * @param lastNetworkPlayerProxyInfo the proxy info to set.
   * @throws NullPointerException if the given proxy info is null.
   */
  public void lastNetworkPlayerProxyInfo(@NonNull NetworkPlayerProxyInfo lastNetworkPlayerProxyInfo) {
    this.lastNetworkPlayerProxyInfo = lastNetworkPlayerProxyInfo;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document propertyHolder() {
    return this.properties;
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
