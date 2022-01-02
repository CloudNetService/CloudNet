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

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class ServiceTask extends ServiceConfigurationBase implements Cloneable, Nameable {

  public static final Pattern NAMING_PATTERN = Pattern.compile("^[a-zA-Z0-9._\\-*]*$");

  private final String name;
  private final String runtime;
  private final String javaCommand;
  private final String nameSplitter;

  private final boolean disableIpRewrite;
  private final boolean maintenance;
  private final boolean autoDeleteOnStop;
  private final boolean staticServices;

  private final Set<String> groups;
  private final Set<String> associatedNodes;
  private final Set<String> deletedFilesAfterStop;

  private final ProcessConfiguration processConfiguration;

  private final int startPort;
  private final int minServiceCount;

  protected ServiceTask(
    @NonNull String name,
    @NonNull String runtime,
    @Nullable String javaCommand,
    @NonNull String nameSplitter,
    boolean disableIpRewrite,
    boolean maintenance,
    boolean autoDeleteOnStop,
    boolean staticServices,
    @NonNull Set<String> groups,
    @NonNull Set<String> associatedNodes,
    @NonNull Set<String> deletedFilesAfterStop,
    @NonNull ProcessConfiguration processConfiguration,
    int startPort,
    int minServiceCount,
    @NonNull Set<ServiceTemplate> templates,
    @NonNull Set<ServiceDeployment> deployments,
    @NonNull Set<ServiceRemoteInclusion> includes,
    @NonNull JsonDocument properties
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

  public static @NonNull ServiceTask.Builder builder() {
    return new Builder();
  }

  public static @NonNull ServiceTask.Builder builder(@NonNull ServiceTask serviceTask) {
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
  public @NonNull String name() {
    return this.name;
  }

  public @NonNull String runtime() {
    return this.runtime;
  }

  public @Nullable String javaCommand() {
    return this.javaCommand;
  }

  public @NonNull String nameSplitter() {
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
  public @NonNull Collection<String> jvmOptions() {
    return this.processConfiguration.jvmOptions();
  }

  @Override
  public @NonNull Collection<String> processParameters() {
    return this.processConfiguration.processParameters();
  }

  public @NonNull Collection<String> groups() {
    return this.groups;
  }

  public @NonNull Collection<String> associatedNodes() {
    return this.associatedNodes;
  }

  public @NonNull Collection<String> deletedFilesAfterStop() {
    return this.deletedFilesAfterStop;
  }

  public @NonNull ProcessConfiguration processConfiguration() {
    return this.processConfiguration;
  }

  public @Range(from = 1, to = 65535) int startPort() {
    return this.startPort;
  }

  public @Range(from = 0, to = Integer.MAX_VALUE) int minServiceCount() {
    return this.minServiceCount;
  }

  @Override
  public @NonNull ServiceTask clone() {
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

    private Set<String> groups = new HashSet<>();
    private Set<String> associatedNodes = new HashSet<>();
    private Set<String> deletedFilesAfterStop = new HashSet<>();

    private ProcessConfiguration.Builder processConfiguration = ProcessConfiguration.builder();

    private int startPort = -1;
    private int minServiceCount = 0;

    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    public @NonNull Builder runtime(@Nullable String runtime) {
      this.runtime = runtime;
      return this;
    }

    public @NonNull Builder javaCommand(@Nullable String javaCommand) {
      this.javaCommand = javaCommand;
      return this;
    }

    public @NonNull Builder nameSplitter(@NonNull String nameSplitter) {
      this.nameSplitter = nameSplitter;
      return this;
    }

    public @NonNull Builder disableIpRewrite(boolean disableIpRewrite) {
      this.disableIpRewrite = disableIpRewrite;
      return this;
    }

    public @NonNull Builder maintenance(boolean maintenance) {
      this.maintenance = maintenance;
      return this;
    }

    public @NonNull Builder autoDeleteOnStop(boolean autoDeleteOnStop) {
      this.autoDeleteOnStop = autoDeleteOnStop;
      return this;
    }

    public @NonNull Builder staticServices(boolean staticServices) {
      this.staticServices = staticServices;
      return this;
    }

    public @NonNull Builder associatedNodes(@NonNull Collection<String> associatedNodes) {
      this.associatedNodes = new HashSet<>(associatedNodes);
      return this;
    }

    public @NonNull Builder addAssociatedNode(@NonNull String associatedNode) {
      this.associatedNodes.add(associatedNode);
      return this;
    }

    public @NonNull Builder groups(@NonNull Collection<String> groups) {
      this.groups = new HashSet<>(groups);
      return this;
    }

    public @NonNull Builder addGroup(@NonNull String group) {
      this.groups.add(group);
      return this;
    }

    public @NonNull Builder deletedFilesAfterStop(@NonNull Collection<String> deletedFilesAfterStop) {
      this.deletedFilesAfterStop = new HashSet<>(deletedFilesAfterStop);
      return this;
    }

    public @NonNull Builder addDeletedFileAfterStop(@NonNull String deletedFileAfterStop) {
      this.deletedFilesAfterStop.add(deletedFileAfterStop);
      return this;
    }

    public @NonNull Builder processConfiguration(@NonNull ProcessConfiguration.Builder processConfiguration) {
      this.processConfiguration = processConfiguration;
      return this;
    }

    public @NonNull Builder startPort(int startPort) {
      this.startPort = startPort;
      return this;
    }

    public @NonNull Builder minServiceCount(int minServiceCount) {
      this.minServiceCount = minServiceCount;
      return this;
    }

    public @NonNull Builder maxHeapMemory(int maxHeapMemory) {
      this.processConfiguration.maxHeapMemorySize(maxHeapMemory);
      return this;
    }

    public @NonNull Builder serviceEnvironmentType(@NonNull ServiceEnvironmentType serviceEnvironmentType) {
      this.processConfiguration.environment(serviceEnvironmentType);
      this.startPort = this.startPort == -1 ? serviceEnvironmentType.defaultStartPort() : this.startPort;

      return this;
    }

    @Override
    protected @NonNull Builder self() {
      return this;
    }

    @Override
    public @NonNull ServiceTask build() {
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
