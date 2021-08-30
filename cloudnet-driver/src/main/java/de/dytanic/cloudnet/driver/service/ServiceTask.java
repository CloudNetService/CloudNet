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
import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class ServiceTask extends ServiceConfigurationBase implements INameable {

  private String name;
  private String runtime;
  private String javaCommand;

  private boolean disableIpRewrite;
  private boolean maintenance;
  private boolean autoDeleteOnStop;
  private boolean staticServices;

  private Collection<String> associatedNodes = new ArrayList<>();
  private Collection<String> groups = new ArrayList<>();
  private Collection<String> deletedFilesAfterStop = new ArrayList<>();

  private ProcessConfiguration processConfiguration;

  private int startPort;
  private int minServiceCount = 0;

  /**
   * Represents the time in millis where this task is able to start new services again
   */
  private transient long serviceStartAbilityTime = -1;

  protected ServiceTask() {
    super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
  }

  @Deprecated
  @ScheduledForRemoval(inVersion = "3.6")
  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes,
    Collection<String> groups,
    ProcessConfiguration processConfiguration, int startPort, int minServiceCount) {
    this(includes, templates, deployments, name, runtime, false, autoDeleteOnStop,
      staticServices, associatedNodes, groups, processConfiguration, startPort, minServiceCount);
  }

  @Deprecated
  @ScheduledForRemoval(inVersion = "3.6")
  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes,
    Collection<String> groups,
    Collection<String> deletedFilesAfterStop, ProcessConfiguration processConfiguration, int startPort,
    int minServiceCount) {
    this(includes, templates, deployments, name, runtime, false, autoDeleteOnStop, staticServices, associatedNodes,
      groups, deletedFilesAfterStop, processConfiguration, startPort, minServiceCount);
  }

  @Deprecated
  @ScheduledForRemoval(inVersion = "3.6")
  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean autoDeleteOnStop, boolean staticServices, Collection<String> associatedNodes,
    Collection<String> groups,
    Collection<String> deletedFilesAfterStop, ProcessConfiguration processConfiguration, int startPort,
    int minServiceCount, String javaCommand) {
    this(includes, templates, deployments, name, runtime, false, autoDeleteOnStop, staticServices, associatedNodes,
      groups, deletedFilesAfterStop, processConfiguration, startPort, minServiceCount, javaCommand);
  }

  @Deprecated
  @ScheduledForRemoval(inVersion = "3.6")
  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean maintenance, boolean autoDeleteOnStop, boolean staticServices,
    Collection<String> associatedNodes, Collection<String> groups,
    ProcessConfiguration processConfiguration, int startPort, int minServiceCount) {
    this(includes, templates, deployments, name, runtime, maintenance, autoDeleteOnStop, staticServices,
      associatedNodes, groups, new ArrayList<>(), processConfiguration, startPort, minServiceCount);
  }

  @Deprecated
  @ScheduledForRemoval(inVersion = "3.6")
  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean maintenance, boolean autoDeleteOnStop, boolean staticServices,
    Collection<String> associatedNodes, Collection<String> groups,
    Collection<String> deletedFilesAfterStop, ProcessConfiguration processConfiguration, int startPort,
    int minServiceCount) {
    this(includes, templates, deployments, name, runtime, maintenance, autoDeleteOnStop, staticServices,
      associatedNodes, groups, deletedFilesAfterStop, processConfiguration, startPort, minServiceCount, null);
  }

  @Deprecated
  @ScheduledForRemoval(inVersion = "3.6")
  public ServiceTask(Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    String name, String runtime, boolean maintenance, boolean autoDeleteOnStop, boolean staticServices,
    Collection<String> associatedNodes, Collection<String> groups,
    Collection<String> deletedFilesAfterStop, ProcessConfiguration processConfiguration, int startPort,
    int minServiceCount, String javaCommand) {
    super(includes, templates, deployments);
    this.name = name;
    this.runtime = runtime;
    this.maintenance = maintenance;
    this.autoDeleteOnStop = autoDeleteOnStop;
    this.associatedNodes = associatedNodes;
    this.groups = groups;
    this.deletedFilesAfterStop = deletedFilesAfterStop;
    this.processConfiguration = processConfiguration;
    this.startPort = startPort;
    this.minServiceCount = minServiceCount;
    this.staticServices = staticServices;
    this.javaCommand = javaCommand;
  }

  @NotNull
  public static ServiceTask.Builder builder() {
    return new Builder();
  }

  @NotNull
  public static ServiceTask.Builder builder(@NotNull ServiceTask serviceTask) {
    return new Builder(serviceTask);
  }

  @Override
  public Collection<String> getJvmOptions() {
    return this.processConfiguration.getJvmOptions();
  }

  @Override
  public Collection<String> getProcessParameters() {
    return this.processConfiguration.getProcessParameters();
  }

  /**
   * Forbids this task to auto start new services for a specific time on the current node. This method has no effect
   * when executed on a wrapper instances.
   *
   * @param time the time in millis
   */
  public void forbidServiceStarting(long time) {
    this.serviceStartAbilityTime = System.currentTimeMillis() + time;
  }

  public boolean canStartServices() {
    return !this.maintenance && System.currentTimeMillis() > this.serviceStartAbilityTime;
  }

  public boolean isDisableIpRewrite() {
    return this.disableIpRewrite;
  }

  public void setDisableIpRewrite(boolean disableIpRewrite) {
    this.disableIpRewrite = disableIpRewrite;
  }

  public @NotNull String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Nullable
  public String getJavaCommand() {
    return this.javaCommand;
  }

  public void setJavaCommand(String javaCommand) {
    this.javaCommand = javaCommand;
  }

  public String getRuntime() {
    return this.runtime;
  }

  public void setRuntime(String runtime) {
    this.runtime = runtime;
  }

  public boolean isMaintenance() {
    return this.maintenance;
  }

  public void setMaintenance(boolean maintenance) {
    this.maintenance = maintenance;
  }

  public boolean isAutoDeleteOnStop() {
    return this.autoDeleteOnStop;
  }

  public void setAutoDeleteOnStop(boolean autoDeleteOnStop) {
    this.autoDeleteOnStop = autoDeleteOnStop;
  }

  public boolean isStaticServices() {
    return this.staticServices;
  }

  public void setStaticServices(boolean staticServices) {
    this.staticServices = staticServices;
  }

  public Collection<String> getAssociatedNodes() {
    return this.associatedNodes;
  }

  public void setAssociatedNodes(Collection<String> associatedNodes) {
    this.associatedNodes = associatedNodes;
  }

  public Collection<String> getGroups() {
    return this.groups;
  }

  public void setGroups(Collection<String> groups) {
    this.groups = groups;
  }

  public Collection<String> getDeletedFilesAfterStop() {
    return this.deletedFilesAfterStop;
  }

  public void setDeletedFilesAfterStop(Collection<String> deletedFilesAfterStop) {
    this.deletedFilesAfterStop = deletedFilesAfterStop;
  }

  public ProcessConfiguration getProcessConfiguration() {
    return this.processConfiguration;
  }

  public void setProcessConfiguration(ProcessConfiguration processConfiguration) {
    this.processConfiguration = processConfiguration;
  }

  public int getStartPort() {
    return this.startPort;
  }

  public void setStartPort(int startPort) {
    this.startPort = startPort;
  }

  public int getMinServiceCount() {
    return this.minServiceCount;
  }

  public void setMinServiceCount(int minServiceCount) {
    this.minServiceCount = minServiceCount;
  }

  public ServiceTask makeClone() {
    return ServiceTask.builder()
      .includes(new ArrayList<>(this.includes))
      .templates(new ArrayList<>(this.templates))
      .deployments(new ArrayList<>(this.deployments))
      .name(this.name)
      .runtime(this.runtime)
      .maintenance(this.maintenance)
      .autoDeleteOnStop(this.autoDeleteOnStop)
      .staticServices(this.staticServices)
      .associatedNodes(new ArrayList<>(this.associatedNodes))
      .groups(new ArrayList<>(this.groups))
      .deletedFilesAfterStop(new ArrayList<>(this.deletedFilesAfterStop))
      .serviceEnvironmentType(this.processConfiguration.environment)
      .maxHeapMemory(this.processConfiguration.maxHeapMemorySize)
      .jvmOptions(new ArrayList<>(this.processConfiguration.jvmOptions))
      .processParameters(new ArrayList<>(this.processConfiguration.processParameters))
      .startPort(this.startPort)
      .minServiceCount(this.minServiceCount)
      .javaCommand(this.javaCommand)
      .build();
  }

  public static class Builder {

    private final ServiceTask serviceTask;

    protected Builder() {
      this.serviceTask = new ServiceTask();
      this.serviceTask.processConfiguration = new ProcessConfiguration();
    }

    protected Builder(@NotNull ServiceTask serviceTask) {
      Preconditions.checkNotNull(serviceTask, "serviceTask");

      this.serviceTask = serviceTask;
    }

    @NotNull
    public Builder name(@NotNull String name) {
      Preconditions.checkNotNull(name, "name");

      this.serviceTask.setName(name);
      return this;
    }

    @NotNull
    public Builder runtime(@Nullable String runtime) {
      this.serviceTask.setRuntime(runtime);
      return this;
    }

    @NotNull
    public Builder javaCommand(@Nullable String javaCommand) {
      this.serviceTask.setJavaCommand(javaCommand);
      return this;
    }

    @NotNull
    public Builder disableIpRewrite(boolean disableIpRewrite) {
      this.serviceTask.setDisableIpRewrite(disableIpRewrite);
      return this;
    }

    @NotNull
    public Builder maintenance(boolean maintenance) {
      this.serviceTask.setMaintenance(maintenance);
      return this;
    }

    @NotNull
    public Builder autoDeleteOnStop(boolean autoDeleteOnStop) {
      this.serviceTask.setAutoDeleteOnStop(autoDeleteOnStop);
      return this;
    }

    @NotNull
    public Builder staticServices(boolean staticServices) {
      this.serviceTask.setStaticServices(staticServices);
      return this;
    }

    @NotNull
    public Builder associatedNodes(@NotNull Collection<String> associatedNodes) {
      Preconditions.checkNotNull(associatedNodes, "associatedNodes");

      this.serviceTask.setAssociatedNodes(new ArrayList<>(associatedNodes));
      return this;
    }

    @NotNull
    public Builder groups(@NotNull Collection<String> groups) {
      Preconditions.checkNotNull(groups, "groups");

      this.serviceTask.setGroups(new ArrayList<>(groups));
      return this;
    }

    @NotNull
    public Builder deletedFilesAfterStop(@NotNull Collection<String> deletedFilesAfterStop) {
      Preconditions.checkNotNull(deletedFilesAfterStop, "deletedFilesAfterStop");

      this.serviceTask.setDeletedFilesAfterStop(new ArrayList<>(deletedFilesAfterStop));
      return this;
    }

    @NotNull
    public Builder processConfiguration(@NotNull ProcessConfiguration processConfiguration) {
      Preconditions.checkNotNull(processConfiguration, "processConfiguration");

      this.serviceTask.setProcessConfiguration(processConfiguration.clone());
      return this;
    }

    @NotNull
    public Builder startPort(int startPort) {
      this.serviceTask.setStartPort(startPort);
      return this;
    }

    @NotNull
    public Builder minServiceCount(int minServiceCount) {
      this.serviceTask.setMinServiceCount(minServiceCount);
      return this;
    }

    @NotNull
    public Builder jvmOptions(@NotNull Collection<String> jvmOptions) {
      Preconditions.checkNotNull(jvmOptions, "jvmOptions");

      this.serviceTask.processConfiguration.setJvmOptions(new ArrayList<>(jvmOptions));
      return this;
    }

    @NotNull
    public Builder processParameters(@NotNull Collection<String> processParameters) {
      Preconditions.checkNotNull(processParameters, "processParameters");

      this.serviceTask.processConfiguration.setProcessParameters(new ArrayList<>(processParameters));
      return this;
    }

    @NotNull
    public Builder includes(@NotNull Collection<ServiceRemoteInclusion> includes) {
      Preconditions.checkNotNull(includes, "includes");

      this.serviceTask.setIncludes(new ArrayList<>(includes));
      return this;
    }

    @NotNull
    public Builder templates(@NotNull Collection<ServiceTemplate> templates) {
      Preconditions.checkNotNull(templates, "templates");

      this.serviceTask.setTemplates(new ArrayList<>(templates));
      return this;
    }

    @NotNull
    public Builder deployments(@NotNull Collection<ServiceDeployment> deployments) {
      Preconditions.checkNotNull(deployments, "deployments");

      this.serviceTask.setDeployments(new ArrayList<>(deployments));
      return this;
    }

    @NotNull
    public Builder maxHeapMemory(int maxHeapMemory) {
      this.serviceTask.processConfiguration.setMaxHeapMemorySize(maxHeapMemory);
      return this;
    }

    @NotNull
    public Builder serviceEnvironmentType(@NotNull ServiceEnvironmentType serviceEnvironmentType) {
      Preconditions.checkNotNull(serviceEnvironmentType, "serviceEnvironmentType");

      this.serviceTask.processConfiguration.setEnvironment(serviceEnvironmentType);
      return this;
    }

    @NotNull
    public ServiceTask build() {
      Preconditions.checkNotNull(this.serviceTask.name, "name");
      Preconditions.checkNotNull(this.serviceTask.processConfiguration, "processConfiguration");
      Preconditions.checkNotNull(this.serviceTask.processConfiguration.environment, "environment");
      Preconditions.checkArgument(this.serviceTask.processConfiguration.maxHeapMemorySize > 0, "maxHeapMemory < 0");
      Preconditions.checkArgument(this.serviceTask.startPort > 0, "startPort < 0");

      if (this.serviceTask.runtime == null) {
        this.serviceTask.runtime = "jvm";
      }
      if (this.serviceTask.associatedNodes == null) {
        this.serviceTask.associatedNodes = new ArrayList<>();
      }
      if (this.serviceTask.groups == null) {
        this.serviceTask.groups = new ArrayList<>();
      }
      if (this.serviceTask.deletedFilesAfterStop == null) {
        this.serviceTask.deletedFilesAfterStop = new ArrayList<>();
      }
      if (this.serviceTask.includes == null) {
        this.serviceTask.includes = new ArrayList<>();
      }
      if (this.serviceTask.templates == null) {
        this.serviceTask.templates = new ArrayList<>();
      }
      if (this.serviceTask.deployments == null) {
        this.serviceTask.deployments = new ArrayList<>();
      }
      if (this.serviceTask.processConfiguration.processParameters == null) {
        this.serviceTask.processConfiguration.processParameters = new ArrayList<>();
      }
      if (this.serviceTask.processConfiguration.jvmOptions == null) {
        this.serviceTask.processConfiguration.jvmOptions = new ArrayList<>();
      }

      return this.serviceTask;
    }
  }
}
