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
import java.util.Collection;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode
public class ServiceId implements INameable {

  protected UUID uniqueId;

  protected String taskName;
  protected String nameSplitter;
  protected int taskServiceId = -1;

  protected String nodeUniqueId;
  protected Collection<String> allowedNodes;

  protected ServiceEnvironmentType environment;

  public ServiceId(
    @NotNull UUID uniqueId,
    @NotNull String taskName,
    @NotNull String nameSplitter,
    int taskServiceId,
    String nodeUniqueId,
    Collection<String> allowedNodes,
    ServiceEnvironmentType environment
  ) {
    this.uniqueId = uniqueId;
    this.taskName = taskName;
    this.nameSplitter = nameSplitter;
    this.taskServiceId = taskServiceId;
    this.nodeUniqueId = nodeUniqueId;
    this.allowedNodes = allowedNodes;
    this.environment = environment;
  }

  ServiceId() {
  }

  @Override
  public @NotNull String getName() {
    return this.taskName + this.nameSplitter + this.taskServiceId;
  }

  public UUID getUniqueId() {
    return this.uniqueId;
  }

  public String getNodeUniqueId() {
    return this.nodeUniqueId;
  }

  @Internal
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

  @Internal
  public void setTaskServiceId(int taskServiceId) {
    this.taskServiceId = taskServiceId;
  }

  public ServiceEnvironmentType getEnvironment() {
    return this.environment;
  }

  @Override
  public String toString() {
    return this.getName() + ':' + this.uniqueId;
  }
}
