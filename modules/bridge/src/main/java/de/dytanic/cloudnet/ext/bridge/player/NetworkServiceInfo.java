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
import org.jetbrains.annotations.NotNull;

public record NetworkServiceInfo(@NotNull Set<String> groups, @NotNull ServiceId serviceId) {

  public NetworkServiceInfo(@NotNull Set<String> groups, @NotNull ServiceId serviceId) {
    this.groups = groups;
    this.serviceId = serviceId;
  }

  public @NotNull ServiceEnvironmentType environment() {
    return this.serviceId.environment();
  }

  public @NotNull UUID uniqueId() {
    return this.serviceId.uniqueId();
  }

  public @NotNull String serverName() {
    return this.serviceId.name();
  }

  public @NotNull String taskName() {
    return this.serviceId.taskName();
  }
}
