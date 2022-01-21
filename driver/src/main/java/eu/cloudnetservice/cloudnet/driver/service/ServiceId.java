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

package eu.cloudnetservice.cloudnet.driver.service;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.cloudnet.common.Nameable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@EqualsAndHashCode
public class ServiceId implements Nameable {

  protected final String taskName;
  protected final String nameSplitter;
  protected final Set<String> allowedNodes;

  protected final UUID uniqueId;
  protected final int taskServiceId;
  protected final String nodeUniqueId;

  protected final String environmentName;
  protected final ServiceEnvironmentType environment;

  protected ServiceId(
    @NonNull String taskName,
    @NonNull String nameSplitter,
    @NonNull Set<String> allowedNodes,
    @NonNull UUID uniqueId,
    int taskServiceId,
    @Nullable String nodeUniqueId,
    @NonNull String environmentName,
    @Nullable ServiceEnvironmentType environment
  ) {
    this.uniqueId = uniqueId;
    this.taskName = taskName;
    this.nameSplitter = nameSplitter;
    this.taskServiceId = taskServiceId;
    this.nodeUniqueId = nodeUniqueId;
    this.allowedNodes = allowedNodes;
    this.environmentName = environmentName;
    this.environment = environment;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ServiceId serviceId) {
    return builder()
      .uniqueId(serviceId.uniqueId())
      .taskName(serviceId.taskName())
      .nameSplitter(serviceId.nameSplitter())
      .environment(serviceId.environmentName())
      .environment(serviceId.environment())
      .taskServiceId(serviceId.taskServiceId())
      .nodeUniqueId(serviceId.nodeUniqueId())
      .allowedNodes(serviceId.allowedNodes());
  }

  @Override
  public @NonNull String name() {
    return this.taskName + this.nameSplitter + this.taskServiceId;
  }

  public @NonNull UUID uniqueId() {
    return this.uniqueId;
  }

  public @UnknownNullability String nodeUniqueId() {
    return this.nodeUniqueId;
  }

  public @NonNull Collection<String> allowedNodes() {
    return this.allowedNodes;
  }

  public @NonNull String taskName() {
    return this.taskName;
  }

  public @NonNull String nameSplitter() {
    return this.nameSplitter;
  }

  public @NonNull String environmentName() {
    return this.environmentName;
  }

  public int taskServiceId() {
    return this.taskServiceId;
  }

  public @UnknownNullability ServiceEnvironmentType environment() {
    return this.environment;
  }

  @Override
  public String toString() {
    return this.name() + ':' + this.uniqueId;
  }

  public static class Builder {

    protected UUID uniqueId = UUID.randomUUID();

    protected String taskName;
    protected int taskServiceId = -1;
    protected String nodeUniqueId;
    protected String environmentName;
    protected String nameSplitter = "-";

    protected ServiceEnvironmentType environment;
    protected Set<String> allowedNodes = new HashSet<>();

    public @NonNull Builder uniqueId(@NonNull UUID uniqueId) {
      this.uniqueId = uniqueId;
      return this;
    }

    public @NonNull Builder taskName(@NonNull String taskName) {
      this.taskName = taskName;
      return this;
    }

    public @NonNull Builder taskServiceId(int taskServiceId) {
      this.taskServiceId = taskServiceId;
      return this;
    }

    public @NonNull Builder nodeUniqueId(@Nullable String nodeUniqueId) {
      this.nodeUniqueId = nodeUniqueId;
      return this;
    }

    public @NonNull Builder nameSplitter(@NonNull String nameSplitter) {
      this.nameSplitter = nameSplitter;
      return this;
    }

    public @NonNull Builder environment(@NonNull String environmentName) {
      this.environmentName = environmentName;
      return this;
    }

    public @NonNull Builder environment(@Nullable ServiceEnvironmentType environment) {
      if (environment != null) {
        this.environment = environment;
        this.environmentName = environment.name();
      }
      return this;
    }

    public @NonNull Builder allowedNodes(@NonNull Collection<String> allowedNodes) {
      this.allowedNodes = new HashSet<>(allowedNodes);
      return this;
    }

    public @NonNull Builder addAllowedNode(@NonNull String nodeUniqueId) {
      this.allowedNodes.add(nodeUniqueId);
      return this;
    }

    public @NonNull ServiceId build() {
      Preconditions.checkNotNull(this.taskName, "no task name given");
      Preconditions.checkNotNull(this.environmentName, "no environment given");
      Preconditions.checkArgument(this.taskServiceId == -1 || this.taskServiceId > 0, "taskServiceId <= 0");

      return new ServiceId(
        this.taskName,
        this.nameSplitter,
        this.allowedNodes,
        this.uniqueId,
        this.taskServiceId,
        this.nodeUniqueId,
        this.environmentName,
        this.environment);
    }
  }
}
