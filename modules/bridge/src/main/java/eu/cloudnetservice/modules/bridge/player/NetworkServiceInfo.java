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
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;

/**
 * A network service info represents a small portion of a {@link ServiceInfoSnapshot}.
 *
 * @param groups    the groups of the represented service.
 * @param serviceId the service id of the represented service.
 * @see ServiceId
 * @since 4.0
 */
public record NetworkServiceInfo(@NonNull Set<String> groups, @NonNull ServiceId serviceId) {

  /**
   * Creates a new network service info with groups and the service id from the given service.
   *
   * @param snapshot the snapshot to create a network service from.
   * @return the new network service info for the given service info.
   * @throws NullPointerException if the given snapshot is null.
   */
  public static @NonNull NetworkServiceInfo fromServiceInfoSnapshot(@NonNull ServiceInfoSnapshot snapshot) {
    return new NetworkServiceInfo(snapshot.configuration().groups(), snapshot.serviceId());
  }

  /**
   * Gets the environment of the given service from the given service id.
   *
   * @return the environment of the service.
   * @see ServiceId#environment()
   */
  public @NonNull ServiceEnvironmentType environment() {
    return this.serviceId.environment();
  }

  /**
   * Gets the unique id of the given service from the given service id.
   *
   * @return the unique id of the service.
   * @see ServiceId#uniqueId()
   */
  public @NonNull UUID uniqueId() {
    return this.serviceId.uniqueId();
  }

  /**
   * Gets the name of the given service from the given service id.
   *
   * @return the name of the given service.
   * @see ServiceId#name()
   */
  public @NonNull String serverName() {
    return this.serviceId.name();
  }

  /**
   * Gets the name of the task the given service is associated with.
   *
   * @return the task name of the given service.
   * @see ServiceId#taskName()
   */
  public @NonNull String taskName() {
    return this.serviceId.taskName();
  }
}
