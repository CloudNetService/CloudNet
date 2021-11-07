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

import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceId;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public final class NetworkServiceInfo {

  private final Set<String> groups;
  private final ServiceId serviceId;

  public NetworkServiceInfo(@NotNull Set<String> groups, @NotNull ServiceId serviceId) {
    this.groups = groups;
    this.serviceId = serviceId;
  }

  public @NotNull ServiceEnvironmentType getEnvironment() {
    return this.serviceId.getEnvironment();
  }

  public @NotNull UUID getUniqueId() {
    return this.serviceId.getUniqueId();
  }

  public @NotNull String getServerName() {
    return this.serviceId.getName();
  }

  public @NotNull String getTaskName() {
    return this.serviceId.getTaskName();
  }

  public @NotNull ServiceId getServiceId() {
    return this.serviceId;
  }

  public @NotNull Set<String> getGroups() {
    return this.groups;
  }
}
