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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.INameable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@EqualsAndHashCode
public class ServiceId implements INameable {

  protected final String taskName;
  protected final String nameSplitter;
  protected final Set<String> allowedNodes;
  protected final ServiceEnvironmentType environment;

  protected volatile UUID uniqueId;
  protected volatile int taskServiceId;
  protected volatile String nodeUniqueId;

  protected ServiceId(
    @NotNull String taskName,
    @NotNull String nameSplitter,
    @NotNull Set<String> allowedNodes,
    @NotNull ServiceEnvironmentType environment,
    @NotNull UUID uniqueId,
    int taskServiceId,
    @Nullable String nodeUniqueId
  ) {
    this.uniqueId = uniqueId;
    this.taskName = taskName;
    this.nameSplitter = nameSplitter;
    this.taskServiceId = taskServiceId;
    this.nodeUniqueId = nodeUniqueId;
    this.allowedNodes = allowedNodes;
    this.environment = environment;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  @Override
  public @NotNull String getName() {
    return this.taskName + this.nameSplitter + this.taskServiceId;
  }

  public @NotNull UUID getUniqueId() {
    return this.uniqueId;
  }

  @Internal
  public void setUniqueId(@NotNull UUID uniqueId) {
    this.uniqueId = uniqueId;
  }

  public @UnknownNullability String getNodeUniqueId() {
    return this.nodeUniqueId;
  }

  @Internal
  public void setNodeUniqueId(@NotNull String nodeUniqueId) {
    this.nodeUniqueId = nodeUniqueId;
  }

  public @NotNull Collection<String> getAllowedNodes() {
    return this.allowedNodes;
  }

  public @NotNull String getTaskName() {
    return this.taskName;
  }

  public int getTaskServiceId() {
    return this.taskServiceId;
  }

  @Internal
  public void setTaskServiceId(int taskServiceId) {
    this.taskServiceId = taskServiceId;
  }

  public @NotNull ServiceEnvironmentType getEnvironment() {
    return this.environment;
  }

  @Override
  public String toString() {
    return this.getName() + ':' + this.uniqueId;
  }

  public static class Builder {

    protected UUID uniqueId = UUID.randomUUID();

    protected String taskName;
    protected int taskServiceId = -1;
    protected String nodeUniqueId;
    protected String nameSplitter = "-";

    protected ServiceEnvironmentType environment;
    protected Set<String> allowedNodes = new HashSet<>();

    public @NotNull Builder uniqueId(@NotNull UUID uniqueId) {
      this.uniqueId = uniqueId;
      return this;
    }

    public @NotNull Builder taskName(@NotNull String taskName) {
      this.taskName = taskName;
      return this;
    }

    public @NotNull Builder taskServiceId(int taskServiceId) {
      this.taskServiceId = taskServiceId;
      return this;
    }

    public @NotNull Builder nodeUniqueId(@Nullable String nodeUniqueId) {
      this.nodeUniqueId = nodeUniqueId;
      return this;
    }

    public @NotNull Builder nameSplitter(@NotNull String nameSplitter) {
      this.nameSplitter = nameSplitter;
      return this;
    }

    public @NotNull Builder environment(@NotNull ServiceEnvironmentType environment) {
      this.environment = environment;
      return this;
    }

    public @NotNull Builder allowedNodes(@NotNull Collection<String> allowedNodes) {
      this.allowedNodes = new HashSet<>(allowedNodes);
      return this;
    }

    public @NotNull Builder addAllowedNode(@NotNull String nodeUniqueId) {
      this.allowedNodes.add(nodeUniqueId);
      return this;
    }

    public @NotNull ServiceId build() {
      Preconditions.checkNotNull(this.taskName, "no task name given");
      Preconditions.checkNotNull(this.environment, "no environment given");
      Preconditions.checkArgument(this.taskServiceId == -1 || this.taskServiceId > 0, "taskServiceId <= 0");

      return new ServiceId(
        this.taskName,
        this.nameSplitter,
        this.allowedNodes,
        this.environment,
        this.uniqueId,
        this.taskServiceId,
        this.nodeUniqueId);
    }
  }
}
