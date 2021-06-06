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

package de.dytanic.cloudnet.driver.network.cluster;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public class NetworkClusterNode extends SerializableJsonDocPropertyable implements SerializableObject {

  private String uniqueId;

  private HostAndPort[] listeners;

  public NetworkClusterNode(String uniqueId, HostAndPort[] listeners) {
    this.uniqueId = uniqueId;
    this.listeners = listeners;
  }

  public NetworkClusterNode() {
  }

  public String getUniqueId() {
    return this.uniqueId;
  }

  public HostAndPort[] getListeners() {
    return this.listeners;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeString(this.uniqueId);
    buffer.writeObjectArray(this.listeners);

    super.write(buffer);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.uniqueId = buffer.readString();
    this.listeners = buffer.readObjectArray(HostAndPort.class);

    super.read(buffer);
  }
}
