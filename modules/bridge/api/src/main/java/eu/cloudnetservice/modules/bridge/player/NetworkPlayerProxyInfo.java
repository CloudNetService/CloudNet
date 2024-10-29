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

import eu.cloudnetservice.driver.network.HostAndPort;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * A network player proxy info represents the proxy service a cloud player is connected to.
 *
 * @param uniqueId       the unique id of the player.
 * @param name           the name of the player.
 * @param xBoxId         the xbox id of the player, null if the player does not have a xbox id.
 * @param version        the protocol version used by the player to connect to the proxy.
 * @param address        the host address of the player used to connect to the proxy.
 * @param listener       the host address the proxy is bound to.
 * @param onlineMode     the online mode of the player, true if the player is authenticated with mojang services.
 * @param networkService the service info of the proxy the player is connected to.
 * @see NetworkServiceInfo
 * @since 4.0
 */
public record NetworkPlayerProxyInfo(
  @NonNull UUID uniqueId,
  @NonNull String name,
  @Nullable String xBoxId,
  @Range(from = 47, to = Integer.MAX_VALUE) int version,
  @NonNull HostAndPort address,
  @NonNull HostAndPort listener,
  boolean onlineMode,
  @NonNull NetworkServiceInfo networkService
) implements Cloneable {

  /**
   * {@inheritDoc}
   */
  @Override
  public NetworkPlayerProxyInfo clone() {
    try {
      return (NetworkPlayerProxyInfo) super.clone();
    } catch (CloneNotSupportedException exception) {
      // this can not happen
      throw new RuntimeException();
    }
  }
}
