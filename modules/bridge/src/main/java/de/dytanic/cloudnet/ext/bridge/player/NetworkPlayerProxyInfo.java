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

import de.dytanic.cloudnet.driver.network.HostAndPort;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public record NetworkPlayerProxyInfo(
  @NotNull UUID uniqueId,
  @NotNull String name,
  @Nullable String xBoxId,
  @Range(from = 47, to = Integer.MAX_VALUE) int version,
  @NotNull HostAndPort address,
  @NotNull HostAndPort listener,
  boolean onlineMode,
  @NotNull NetworkServiceInfo networkService
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
