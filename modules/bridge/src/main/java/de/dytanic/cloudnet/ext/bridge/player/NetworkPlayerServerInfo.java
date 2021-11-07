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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

@ToString
@EqualsAndHashCode
public final class NetworkPlayerServerInfo {

  private final UUID uniqueId;

  private final String name;
  private final String xBoxId;

  private final HostAndPort address;
  private final NetworkServiceInfo networkService;

  public NetworkPlayerServerInfo(UUID uniqueId, String name, String xBoxId, HostAndPort address,
    NetworkServiceInfo networkService) {
    this.uniqueId = uniqueId;
    this.name = name;
    this.xBoxId = xBoxId;
    this.address = address;
    this.networkService = networkService;
  }

  public @NotNull UUID getUniqueId() {
    return this.uniqueId;
  }

  public @NotNull String getName() {
    return this.name;
  }

  public @UnknownNullability String getXBoxId() {
    return this.xBoxId;
  }

  public @NotNull HostAndPort getAddress() {
    return this.address;
  }

  public @NotNull NetworkServiceInfo getNetworkService() {
    return this.networkService;
  }
}
