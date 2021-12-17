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

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class ServiceTask extends ServiceConfigurationBase implements Cloneable, INameable {

  public static final Pattern NAMING_PATTERN = Pattern.compile("^[a-zA-Z0-9._\\-*]*$");

  private final String name;
  private final String runtime;
  private final String javaCommand;
  private final String nameSplitter;

  private final boolean disableIpRewrite;
  private final boolean maintenance;
  private final boolean autoDeleteOnStop;
  private final boolean staticServices;

  private final Collection<String> groups;
  private final Collection<String> associatedNodes;
  private final Collection<String> deletedFilesAfterStop;

  private final ProcessConfiguration processConfiguration;

  private final int startPort;
  private final int minServiceCount;

  protected ServiceTask(
    @NotNull String name,
    @NotNull String runtime,
    @Nullable String javaCommand,
    @NotNull String nameSplitter,
    boolean disableIpRewrite,
    boolean maintenance,
    boolean autoDeleteOnStop,
    boolean staticServices,
    @NotNull Collection<String> groups,
    @NotNull Collection<String> associatedNodes,
    @NotNull Collection<String> deletedFilesAfterStop,
    @NotNull ProcessConfiguration processConfiguration,
    int startPort,
    int minServiceCount,
    @NotNull Collection<ServiceTemplate> templates,
    @NotNull Collection<ServiceDeployment> deployments,
    @NotNull Collection<ServiceRemoteInclusion> includes,
    @NotNull JsonDocument properties
  ) {
    super(templates, deployments, includes, properties);
    this.name = name;
    this.runtime = runtime;
    this.javaCommand = javaCommand;
    this.nameSplitter = nameSplitter;
    this.disableIpRewrite = disableIpRewrite;
    this.maintenance = maintenance;
    this.autoDeleteOnStop = autoDeleteOnStop;
    this.staticServices = staticServices;
    this.associatedNodes = associatedNodes;
    this.groups = groups;
    this.deletedFilesAfterStop = deletedFilesAfterStop;
    this.processConfiguration = processConfiguration;
    this.startPort = startPort;
    this.minServiceCount = minServiceCount;
  }

  public static @NotNull ServiceTask.Builder builder() {
    return new Builder();
  }

  public static @NotNull ServiceTask.Builder builder(@NotNull ServiceTask serviceTask) {
    return builder()
      .name(serviceTask.name())
      .javaCommand(serviceTask.javaCommand())
      .runtime(serviceTask.runtime())
      .nameSplitter(serviceTask.nameSplitter())

      .maintenance(serviceTask.maintenance())
      .staticServices(serviceTask.staticServices())
      .disableIpRewrite(serviceTask.disableIpRewrite())
      .autoDeleteOnStop(serviceTask.autoDeleteOnStop())

      .groups(serviceTask.groups())
      .associatedNodes(serviceTask.associatedNodes())
      .deletedFilesAfterStop(serviceTask.deletedFilesAfterStop())

      .jvmOptions(serviceTask.jvmOptions())
      .processParameters(serviceTask.processParameters())
      .templates(serviceTask.templates())
      .deployments(serviceTask.deployments())
      .includes(serviceTask.includes())

      .startPort(serviceTask.startPort())
      .minServiceCount(serviceTask.minServiceCount())

      .properties(serviceTask.properties().clone())
      .processConfiguration(ProcessConfiguration.builder(serviceTask.processConfiguration()));
  }

  @Override
  public @NotNull String name() {
    return this.name;
  }

  public @NotNull String runtime() {
    return this.runtime;
  }

  public @Nullable String javaCommand() {
    return this.javaCommand;
  }

  public @NotNull String nameSplitter() {
    return this.nameSplitter;
  }

  public boolean disableIpRewrite() {
    return this.disableIpRewrite;
  }

  public boolean maintenance() {
    return this.maintenance;
  }

  public boolean autoDeleteOnStop() {
    return this.autoDeleteOnStop;
  }

  public boolean staticServices() {
    return this.staticServices;
  }

  @Override
  public @NotNull Collection<String> jvmOptions() {
    return this.processConfiguration.jvmOptions();
  }

  @Override
  public @NotNull Collection<String> processParameters() {
    return this.processConfiguration.processParameters();
  }

  public @NotNull Collection<String> groups() {
    return this.groups;
  }

  public @NotNull Collection<String> associatedNodes() {
    return this.associatedNodes;
  }

  public @NotNull Collection<String> deletedFilesAfterStop() {
    return this.deletedFilesAfterStop;
  }

  public @NotNull ProcessConfiguration processConfiguration() {
    return this.processConfiguration;
  }

  public @Range(from = 1, to = 65535) int startPort() {
    return this.startPort;
  }

  public @Range(from = 0, to = Integer.MAX_VALUE) int minServiceCount() {
    return this.minServiceCount;
  }

  @Override
  public @NotNull ServiceTask clone() {
    try {
      return (ServiceTask) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  public static class Builder extends ServiceConfigurationBase.Builder<ServiceTask, Builder> {

    private String name;
    private String javaCommand;
    private String runtime = "jvm";
    private String nameSplitter = "-";

    private boolean maintenance;
    private boolean staticServices;
    private boolean disableIpRewrite;
    private boolean autoDeleteOnStop = true;

    private Collection<String> groups = new ArrayList<>();
    private Collection<String> associatedNodes = new ArrayList<>();
    private Collection<String> deletedFilesAfterStop = new ArrayList<>();

    private ProcessConfiguration.Builder processConfiguration = ProcessConfiguration.builder();

    private int startPort = -1;
    private int minServiceCount = 0;

    public @NotNull Builder name(@NotNull String name) {
      this.name = name;
      return this;
    }

    public @NotNull Builder runtime(@Nullable String runtime) {
      this.runtime = runtime;
      return this;
    }

    public @NotNull Builder javaCommand(@Nullable String javaCommand) {
      this.javaCommand = javaCommand;
      return this;
    }

    public @NotNull Builder nameSplitter(@NotNull String nameSplitter) {
      this.nameSplitter = nameSplitter;
      return this;
    }

    public @NotNull Builder disableIpRewrite(boolean disableIpRewrite) {
      this.disableIpRewrite = disableIpRewrite;
      return this;
    }

    public @NotNull Builder maintenance(boolean maintenance) {
      this.maintenance = maintenance;
      return this;
    }

    public @NotNull Builder autoDeleteOnStop(boolean autoDeleteOnStop) {
      this.autoDeleteOnStop = autoDeleteOnStop;
      return this;
    }

    public @NotNull Builder staticServices(boolean staticServices) {
      this.staticServices = staticServices;
      return this;
    }

    public @NotNull Builder associatedNodes(@NotNull Collection<String> associatedNodes) {
      this.associatedNodes = new HashSet<>(associatedNodes);
      return this;
    }

    public @NotNull Builder addAssociatedNode(@NotNull String associatedNode) {
      this.associatedNodes.add(associatedNode);
      return this;
    }

    public @NotNull Builder groups(@NotNull Collection<String> groups) {
      this.groups = new HashSet<>(groups);
      return this;
    }

    public @NotNull Builder addGroup(@NotNull String group) {
      this.groups.add(group);
      return this;
    }

    public @NotNull Builder deletedFilesAfterStop(@NotNull Collection<String> deletedFilesAfterStop) {
      this.deletedFilesAfterStop = new HashSet<>(deletedFilesAfterStop);
      return this;
    }

    public @NotNull Builder addDeletedFileAfterStop(@NotNull String deletedFileAfterStop) {
      this.deletedFilesAfterStop.add(deletedFileAfterStop);
      return this;
    }

    public @NotNull Builder processConfiguration(@NotNull ProcessConfiguration.Builder processConfiguration) {
      this.processConfiguration = processConfiguration;
      return this;
    }

    public @NotNull Builder startPort(int startPort) {
      this.startPort = startPort;
      return this;
    }

    public @NotNull Builder minServiceCount(int minServiceCount) {
      this.minServiceCount = minServiceCount;
      return this;
    }

    public @NotNull Builder maxHeapMemory(int maxHeapMemory) {
      this.processConfiguration.maxHeapMemorySize(maxHeapMemory);
      return this;
    }

    public @NotNull Builder serviceEnvironmentType(@NotNull ServiceEnvironmentType serviceEnvironmentType) {
      this.processConfiguration.environment(serviceEnvironmentType);
      this.startPort = this.startPort == -1 ? serviceEnvironmentType.defaultStartPort() : this.startPort;

      return this;
    }

    @Override
    protected @NotNull Builder self() {
      return this;
    }

    @Override
    public @NotNull ServiceTask build() {
      Verify.verifyNotNull(this.name, "no name given");
      Verify.verify(this.startPort > 0 && this.startPort <= 65535, "Invalid start port given");

      return new ServiceTask(
        this.name,
        this.runtime,
        this.javaCommand,
        this.nameSplitter,
        this.disableIpRewrite,
        this.maintenance,
        this.autoDeleteOnStop,
        this.staticServices,
        this.groups,
        this.associatedNodes,
        this.deletedFilesAfterStop,
        this.processConfiguration.build(),
        this.startPort,
        this.minServiceCount,
        this.templates,
        this.deployments,
        this.includes,
        this.properties);
    }
  }
}
