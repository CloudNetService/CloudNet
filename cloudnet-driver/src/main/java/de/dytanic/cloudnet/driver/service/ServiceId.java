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

package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import java.util.Collection;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public final class ServiceId implements INameable, SerializableObject {

  protected UUID uniqueId;

  protected String taskName;
  protected int taskServiceId = -1;

  protected String nodeUniqueId;
  protected Collection<String> allowedNodes;

  protected ServiceEnvironmentType environment;

  public ServiceId(@NotNull UUID uniqueId, String nodeUniqueId, String taskName, int taskServiceId,
    ServiceEnvironmentType environment) {
    this(uniqueId, nodeUniqueId, taskName, null, taskServiceId, environment);
  }

  public ServiceId(@NotNull UUID uniqueId, String nodeUniqueId, String taskName, Collection<String> allowedNodes,
    int taskServiceId, ServiceEnvironmentType environment) {
    this.uniqueId = uniqueId;
    this.nodeUniqueId = nodeUniqueId;
    this.taskName = taskName;
    this.allowedNodes = allowedNodes;
    this.taskServiceId = taskServiceId;
    this.environment = environment;
  }

  public ServiceId() {
  }

  public String getName() {
    return this.taskName + "-" + this.taskServiceId;
  }

  public UUID getUniqueId() {
    return this.uniqueId;
  }

  public String getNodeUniqueId() {
    return this.nodeUniqueId;
  }

  @ApiStatus.Internal
  public void setNodeUniqueId(String nodeUniqueId) {
    this.nodeUniqueId = nodeUniqueId;
  }

  public Collection<String> getAllowedNodes() {
    return this.allowedNodes;
  }

  public String getTaskName() {
    return this.taskName;
  }

  public int getTaskServiceId() {
    return this.taskServiceId;
  }

  @ApiStatus.Internal
  public void setTaskServiceId(int taskServiceId) {
    this.taskServiceId = taskServiceId;
  }

  public ServiceEnvironmentType getEnvironment() {
    return this.environment;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeUUID(this.uniqueId);
    buffer.writeOptionalString(this.nodeUniqueId);
    buffer.writeBoolean(this.allowedNodes != null);
    if (this.allowedNodes != null) {
      buffer.writeStringCollection(this.allowedNodes);
    }
    buffer.writeString(this.taskName);
    buffer.writeVarInt(this.taskServiceId);
    buffer.writeEnumConstant(this.environment);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.uniqueId = buffer.readUUID();
    this.nodeUniqueId = buffer.readOptionalString();
    this.allowedNodes = buffer.readBoolean() ? buffer.readStringCollection() : null;
    this.taskName = buffer.readString();
    this.taskServiceId = buffer.readVarInt();
    this.environment = buffer.readEnumConstant(ServiceEnvironmentType.class);
  }
}
