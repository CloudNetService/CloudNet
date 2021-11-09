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
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceConfiguration extends JsonDocPropertyHolder {

  protected ServiceId serviceId;

  protected String runtime;
  protected String javaCommand;

  protected boolean autoDeleteOnStop;
  protected boolean staticService;

  protected int port;
  protected ProcessConfiguration processConfig;

  protected Set<String> groups;
  protected Set<String> deletedFilesAfterStop;

  protected Set<ServiceTemplate> templates;
  protected Set<ServiceDeployment> deployments;
  protected Set<ServiceRemoteInclusion> includes;

  protected ServiceConfiguration() {
  }

  public ServiceConfiguration(
    @NotNull ServiceId serviceId,
    @NotNull String runtime,
    @Nullable String javaCommand,
    boolean autoDeleteOnStop,
    boolean staticService,
    int port,
    @NotNull ProcessConfiguration processConfig,
    @NotNull Set<String> groups,
    @NotNull Set<String> deletedFilesAfterStop,
    @NotNull Set<ServiceTemplate> templates,
    @NotNull Set<ServiceDeployment> deployments,
    @NotNull Set<ServiceRemoteInclusion> includes,
    @NotNull JsonDocument properties
  ) {
    this.serviceId = serviceId;
    this.runtime = runtime;
    this.javaCommand = javaCommand;
    this.autoDeleteOnStop = autoDeleteOnStop;
    this.staticService = staticService;
    this.port = port;
    this.processConfig = processConfig;
    this.groups = groups;
    this.deletedFilesAfterStop = deletedFilesAfterStop;
    this.templates = templates;
    this.deployments = deployments;
    this.includes = includes;
    this.properties = properties;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull ServiceTask task) {
    return builder().task(task);
  }

  public @NotNull ServiceId getServiceId() {
    return this.serviceId;
  }

  public boolean isAutoDeleteOnStop() {
    return this.autoDeleteOnStop;
  }

  public void setAutoDeleteOnStop(boolean autoDeleteOnStop) {
    this.autoDeleteOnStop = autoDeleteOnStop;
  }

  public boolean isStaticService() {
    return this.staticService;
  }

  public void setStaticService(boolean staticService) {
    this.staticService = staticService;
  }

  public @Nullable String getJavaCommand() {
    return this.javaCommand;
  }

  public void setJavaCommand(@Nullable String javaCommand) {
    this.javaCommand = javaCommand;
  }

  public @NotNull String getRuntime() {
    return this.runtime;
  }

  public void setRuntime(@NotNull String runtime) {
    this.runtime = runtime;
  }

  public @NotNull Set<String> getGroups() {
    return this.groups;
  }

  public @NotNull Set<String> getDeletedFilesAfterStop() {
    return this.deletedFilesAfterStop;
  }

  public @NotNull Set<ServiceTemplate> getTemplates() {
    return this.templates;
  }

  public @NotNull Set<ServiceDeployment> getDeployments() {
    return this.deployments;
  }

  public @NotNull Set<ServiceRemoteInclusion> getIncludes() {
    return this.includes;
  }

  public @NotNull ProcessConfiguration getProcessConfig() {
    return this.processConfig;
  }

  public @Range(from = 0, to = 65535) int getPort() {
    return this.port;
  }

  public void setPort(@Range(from = 0, to = 65535) int port) {
    this.port = port;
  }

  public @Nullable ServiceInfoSnapshot createNewService() {
    return CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(this);
  }

  public @NotNull ITask<ServiceInfoSnapshot> createNewServiceAsync() {
    return CloudNetDriver.getInstance().getCloudServiceFactory().createCloudServiceAsync(this);
  }

  public boolean isValid() {
    return this.serviceId.taskName != null
      && this.serviceId.environment != null
      && this.processConfig.maxHeapMemorySize > 0
      && this.port >= 0
      && this.port <= 65535;
  }

  public void replaceNulls() {
    if (this.groups == null) {
      this.groups = new HashSet<>();
    }
    if (this.deletedFilesAfterStop == null) {
      this.deletedFilesAfterStop = new HashSet<>();
    }
    if (this.templates == null) {
      this.templates = new HashSet<>();
    }
    if (this.deployments == null) {
      this.deployments = new HashSet<>();
    }
    if (this.includes == null) {
      this.includes = new HashSet<>();
    }
    if (this.serviceId.uniqueId == null) {
      this.serviceId.uniqueId = UUID.randomUUID();
    }
    if (this.processConfig.jvmOptions == null) {
      this.processConfig.jvmOptions = new HashSet<>();
    }
  }

  /**
   * Builder for the creation of new services. All required parameters are:
   * <ul>
   *     <li>{@link #task(String)}</li>
   *     <li>{@link #environment(ServiceEnvironmentType)}</li>
   *     <li>{@link #maxHeapMemory(int)}</li>
   * </ul>
   * You can create a new service with this example:
   * <p>
   * <code>
   *   ServiceConfiguration
   *    .builder()
   *    .task("Lobby")
   *    .environment(ServiceEnvironmentType.MINECRAFT_SERVER)
   *    .maxHeapMemory(512)
   *    .build()
   *    .createNewService();
   * </code>
   * <p>
   * this will return the newly created {@link ServiceInfoSnapshot} or null if the service couldn't be created.
   */
  public static class Builder {

    private final ServiceConfiguration config;

    private Builder() {
      this.config = new ServiceConfiguration();
      this.config.serviceId = new ServiceId();
      this.config.serviceId.nameSplitter = "-";
      this.config.processConfig = new ProcessConfiguration();
      this.config.port = 44955;
    }

    /**
     * Applies every option of the given {@link ServiceTask} object except for the Properties. This will override every
     * previously set option of this builder.
     */
    @NotNull
    public Builder task(@NotNull ServiceTask task) {
      return this
        .task(task.getName())

        .runtime(task.getRuntime())
        .javaCommand(task.getJavaCommand())
        .nameSplitter(task.getNameSplitter())

        .autoDeleteOnStop(task.isAutoDeleteOnStop())
        .staticService(task.isStaticServices())

        .allowedNodes(task.getAssociatedNodes())
        .groups(task.getGroups())
        .deleteFilesAfterStop(task.getDeletedFilesAfterStop())

        .templates(task.getTemplates())
        .deployments(task.getDeployments())
        .inclusions(task.getIncludes())

        .environment(task.getProcessConfiguration().getEnvironment())
        .maxHeapMemory(task.getProcessConfiguration().getMaxHeapMemorySize())
        .jvmOptions(task.getProcessConfiguration().getJvmOptions())
        .processParameters(task.getProcessConfiguration().getProcessParameters())
        .startPort(task.getStartPort());
    }

    /**
     * The complete {@link ServiceId} for the new service. Calling this method will override all the following method
     * calls:
     * <ul>
     *     <li>{@link #task(String)}</li>
     *     <li>{@link #taskId(int)}</li>
     *     <li>{@link #uniqueId(UUID)}</li>
     *     <li>{@link #environment(ServiceEnvironmentType)}</li>
     *     <li>{@link #node(String)}</li>
     *     <li>{@link #allowedNodes(String...)} / {@link #allowedNodes(Collection)}</li>
     * </ul>
     */
    @NotNull
    public Builder serviceId(@NotNull ServiceId serviceId) {
      this.config.serviceId = serviceId;
      return this;
    }

    /**
     * The task for the new service. No permanent task with that name has to exist. This will NOT use any options of the
     * given task, to do that use {@link #task(ServiceTask)}.
     */
    @NotNull
    public Builder task(@NotNull String task) {
      this.config.serviceId.taskName = task;
      return this;
    }

    /**
     * The environment for the new service.
     */
    @NotNull
    public Builder environment(@NotNull ServiceEnvironmentType environment) {
      this.config.serviceId.environment = environment;
      this.config.processConfig.environment = environment;
      return this;
    }

    /**
     * The task id for the new service (For example Lobby-1 would have the task id 1).
     */
    @NotNull
    public Builder taskId(int taskId) {
      this.config.serviceId.taskServiceId = taskId;
      return this;
    }

    /**
     * The uniqueId for the new service.
     */
    @NotNull
    public Builder uniqueId(@NotNull UUID uniqueId) {
      this.config.serviceId.uniqueId = uniqueId;
      return this;
    }

    /**
     * the java command to start the service with if this is null the default value from the config.json will be used
     */
    @NotNull
    public Builder javaCommand(@Nullable String javaCommand) {
      this.config.javaCommand = javaCommand;
      return this;
    }

    @NotNull
    public Builder nameSplitter(@Nullable String nameSplitter) {
      this.config.serviceId.nameSplitter = nameSplitter == null ? "-" : nameSplitter;
      return this;
    }

    /**
     * The node where the new service will start. If the service cannot be created on this node or the node doesn't
     * exist, it will NOT be created and {@link ServiceConfiguration#createNewService()} will return {@code null}.
     */
    @NotNull
    public Builder node(@NotNull String nodeUniqueId) {
      this.config.serviceId.nodeUniqueId = nodeUniqueId;
      return this;
    }

    /**
     * A list of all allowed nodes. CloudNet will choose the node with the most free resources. If a node is provided
     * using {@link #node(String)}, this option will be ignored.
     */
    @NotNull
    public Builder allowedNodes(@NotNull Collection<String> allowedNodes) {
      this.config.serviceId.allowedNodes = new ArrayList<>(allowedNodes);
      return this;
    }

    /**
     * A list of all allowed nodes. CloudNet will choose the node with the most free resources. If a node is provided
     * using {@link #node(String)}, this option will be ignored.
     */
    @NotNull
    public Builder allowedNodes(@NotNull String @NotNull ... allowedNodes) {
      return this.allowedNodes(Arrays.asList(allowedNodes));
    }

    /**
     * A list of all allowed nodes. CloudNet will choose the node with the most free resources. If a node is provided
     * using {@link #node(String)}, this option will be ignored.
     */
    @NotNull
    public Builder addAllowedNodes(@NotNull Collection<String> allowedNodes) {
      if (this.config.serviceId.allowedNodes == null) {
        return this.allowedNodes(allowedNodes);
      }
      this.config.serviceId.allowedNodes.addAll(allowedNodes);
      return this;
    }

    /**
     * A list of all allowed nodes. CloudNet will choose the node with the most free resources. If a node is provided
     * using {@link #node(String)}, this option will be ignored.
     */
    @NotNull
    public Builder addAllowedNodes(@NotNull String @NotNull ... allowedNodes) {
      return this.addAllowedNodes(Arrays.asList(allowedNodes));
    }

    /**
     * The runtime of the service. If none is provided, the default "jvm" is used. By default, CloudNet only provides
     * the "jvm" runtime, you can add your own with custom modules.
     */
    @NotNull
    public Builder runtime(@NotNull String runtime) {
      this.config.runtime = runtime;
      return this;
    }

    /**
     * Whether this service should be deleted on stop (doesn't affect files of a static service) or the life cycle
     * should be changed to {@link ServiceLifeCycle#PREPARED}.
     */
    @NotNull
    public Builder autoDeleteOnStop(boolean autoDeleteOnStop) {
      this.config.autoDeleteOnStop = autoDeleteOnStop;
      return this;
    }

    /**
     * Alias for {@code autoDeleteOnStop(true)}.
     */
    @NotNull
    public Builder autoDeleteOnStop() {
      return this.autoDeleteOnStop(true);
    }

    /**
     * Whether the files should be deleted or saved on deletion of the service.
     */
    @NotNull
    public Builder staticService(boolean staticService) {
      this.config.staticService = staticService;
      return this;
    }

    /**
     * Alias for {@code staticService(true)}.
     */
    @NotNull
    public Builder staticService() {
      return this.staticService(true);
    }

    /**
     * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
     * to the new service.
     */
    @NotNull
    public Builder groups(@NotNull String @NotNull ... groups) {
      return this.groups(Arrays.asList(groups));
    }

    /**
     * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
     * to the new service.
     */
    @NotNull
    public Builder groups(@NotNull Collection<String> groups) {
      this.config.groups = new HashSet<>(groups);
      return this;
    }

    /**
     * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
     * to the new service.
     */
    @NotNull
    public Builder addGroups(String @NotNull ... groups) {
      return this.addGroups(Arrays.asList(groups));
    }

    /**
     * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
     * to the new service.
     */
    @NotNull
    public Builder addGroups(@NotNull Collection<String> groups) {
      if (this.config.groups == null) {
        return this.groups(groups);
      } else {
        this.config.groups.addAll(groups);
        return this;
      }
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    @NotNull
    public Builder inclusions(ServiceRemoteInclusion @NotNull ... inclusions) {
      return this.inclusions(Arrays.asList(inclusions));
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    @NotNull
    public Builder inclusions(@NotNull Collection<ServiceRemoteInclusion> inclusions) {
      this.config.includes = new HashSet<>(inclusions);
      return this;
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    @NotNull
    public Builder addInclusions(ServiceRemoteInclusion @NotNull ... inclusions) {
      return this.addInclusions(Arrays.asList(inclusions));
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    @NotNull
    public Builder addInclusions(@NotNull Collection<ServiceRemoteInclusion> inclusions) {
      if (this.config.includes == null) {
        return this.inclusions(inclusions);
      } else {
        this.config.includes.addAll(inclusions);
        return this;
      }
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    @NotNull
    public Builder templates(ServiceTemplate @NotNull ... templates) {
      return this.templates(Arrays.asList(templates));
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    @NotNull
    public Builder templates(@NotNull Collection<ServiceTemplate> templates) {
      this.config.templates = new HashSet<>(templates);
      return this;
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    @NotNull
    public Builder addTemplates(ServiceTemplate @NotNull ... templates) {
      return this.addTemplates(Arrays.asList(templates));
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    @NotNull
    public Builder addTemplates(@NotNull Collection<ServiceTemplate> templates) {
      if (this.config.templates == null) {
        return this.templates(templates);
      } else {
        this.config.templates.addAll(templates);
        return this;
      }
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#removeAndExecuteDeployments()}.
     */
    @NotNull
    public Builder deployments(ServiceDeployment @NotNull ... deployments) {
      return this.deployments(Arrays.asList(deployments));
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#removeAndExecuteDeployments()}.
     */
    @NotNull
    public Builder deployments(@NotNull Collection<ServiceDeployment> deployments) {
      this.config.deployments = new HashSet<>(deployments);
      return this;
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#removeAndExecuteDeployments()}.
     */
    @NotNull
    public Builder addDeployments(ServiceDeployment @NotNull ... deployments) {
      return this.addDeployments(Arrays.asList(deployments));
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#removeAndExecuteDeployments()}.
     */
    @NotNull
    public Builder addDeployments(@NotNull Collection<ServiceDeployment> deployments) {
      if (this.config.deployments == null) {
        return this.deployments(deployments);
      } else {
        this.config.deployments.addAll(deployments);
        return this;
      }
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    @NotNull
    public Builder deleteFilesAfterStop(String @NotNull ... deletedFilesAfterStop) {
      return this.deleteFilesAfterStop(Arrays.asList(deletedFilesAfterStop));
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    @NotNull
    public Builder deleteFilesAfterStop(@NotNull Collection<String> deletedFilesAfterStop) {
      this.config.deletedFilesAfterStop = new HashSet<>(deletedFilesAfterStop);
      return this;
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    @NotNull
    public Builder addDeletedFilesAfterStop(@NotNull String @NotNull ... deletedFilesAfterStop) {
      return this.addDeletedFilesAfterStop(Arrays.asList(deletedFilesAfterStop));
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    @NotNull
    public Builder addDeletedFilesAfterStop(@NotNull Collection<String> deletedFilesAfterStop) {
      if (this.config.deletedFilesAfterStop == null) {
        return this.deleteFilesAfterStop(deletedFilesAfterStop);
      } else {
        this.config.deletedFilesAfterStop.addAll(deletedFilesAfterStop);
        return this;
      }
    }

    /**
     * The max heap memory for the new service.
     */
    @NotNull
    public Builder maxHeapMemory(int maxHeapMemory) {
      this.config.processConfig.setMaxHeapMemorySize(maxHeapMemory);
      return this;
    }

    /**
     * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup
     * command.
     */
    @NotNull
    public Builder jvmOptions(@NotNull Collection<String> jvmOptions) {
      this.config.processConfig.jvmOptions = new HashSet<>(jvmOptions);
      return this;
    }

    /**
     * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup
     * command.
     */
    @NotNull
    public Builder jvmOptions(@NotNull String @NotNull ... jvmOptions) {
      return this.jvmOptions(Arrays.asList(jvmOptions));
    }

    /**
     * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup
     * command.
     */
    @NotNull
    public Builder addJvmOptions(@NotNull String @NotNull ... jvmOptions) {
      return this.addJvmOptions(Arrays.asList(jvmOptions));
    }

    /**
     * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup
     * command.
     */
    @NotNull
    public Builder addJvmOptions(@NotNull Collection<String> jvmOptions) {
      if (this.config.processConfig.jvmOptions == null) {
        return this.jvmOptions(jvmOptions);
      }
      this.config.processConfig.jvmOptions.addAll(jvmOptions);
      return this;
    }

    /**
     * The process parameters for the new service. This will be the last parameters that will be added to the command.
     */
    @NotNull
    public Builder processParameters(@NotNull Collection<String> jvmOptions) {
      this.config.processConfig.processParameters = new HashSet<>(jvmOptions);
      return this;
    }

    /**
     * The process parameters for the new service. This will be the last parameters that will be added to the command.
     */
    @NotNull
    public Builder addProcessParameters(@NotNull String @NotNull ... jvmOptions) {
      return this.addProcessParameters(Arrays.asList(jvmOptions));
    }

    /**
     * The process parameters for the new service. This will be the last parameters that will be added to the command.
     */
    @NotNull
    public Builder addProcessParameters(@NotNull Collection<String> jvmOptions) {
      this.config.processConfig.processParameters.addAll(jvmOptions);
      return this;
    }

    /**
     * The start port for the new service. CloudNet will test whether the port is used or not, it will count up 1 while
     * the port is used.
     */
    @NotNull
    public Builder startPort(int startPort) {
      this.config.port = startPort;
      return this;
    }

    /**
     * The default properties of the new service. CloudNet itself completely ignores them, but they can be useful if you
     * want to transport data from the component that has created the service to the new service.
     */
    @NotNull
    public Builder properties(@NotNull JsonDocument properties) {
      this.config.properties = properties;
      return this;
    }

    public boolean isValid() {
      return this.config.isValid();
    }

    @NotNull
    public ServiceConfiguration build() {
      Preconditions.checkNotNull(this.config.serviceId.taskName, "No task provided");
      Preconditions.checkNotNull(this.config.serviceId.environment, "No environment provided");
      Preconditions.checkArgument(this.config.processConfig.maxHeapMemorySize > 0, "No max heap memory provided");
      Preconditions.checkArgument(this.config.port > 0, "StartPort has to greater than 0");

      this.config.replaceNulls();
      return this.config;
    }
  }
}
