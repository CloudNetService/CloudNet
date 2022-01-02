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

package eu.cloudnetservice.cloudnet.driver.network.cluster;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public class NetworkClusterNode extends JsonDocPropertyHolder {

  private final String uniqueId;
  private final HostAndPort[] listeners;

  public NetworkClusterNode(@NonNull String uniqueId, @NonNull HostAndPort[] listeners) {
    this.uniqueId = uniqueId;
    this.listeners = listeners;
  }

  public NetworkClusterNode(
    @NonNull String uniqueId,
    @NonNull HostAndPort[] listeners,
    @NonNull JsonDocument properties
  ) {
    this.uniqueId = uniqueId;
    this.listeners = listeners;
    this.properties = properties;
  }

  public @NonNull String uniqueId() {
    return this.uniqueId;
  }

  public @NonNull HostAndPort[] listeners() {
    return this.listeners;
  }
}
