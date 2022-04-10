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

import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceId;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;

public record NetworkServiceInfo(@NonNull Set<String> groups, @NonNull ServiceId serviceId) {

  public @NonNull ServiceEnvironmentType environment() {
    return this.serviceId.environment();
  }

  public @NonNull UUID uniqueId() {
    return this.serviceId.uniqueId();
  }

  public @NonNull String serverName() {
    return this.serviceId.name();
  }

  public @NonNull String taskName() {
    return this.serviceId.taskName();
  }
}
