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
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceConfiguration extends BasicJsonDocPropertyable {

  protected ServiceId serviceId;

  protected String runtime;
  protected String javaCommand;

  protected boolean autoDeleteOnStop;
  protected boolean staticService;

  protected int port;
  protected ProcessConfiguration processConfig;

  protected String[] groups;
  protected String[] deletedFilesAfterStop;

  protected ServiceTemplate[] templates;
  protected ServiceDeployment[] deployments;
  protected ServiceRemoteInclusion[] includes;

  private ServiceTemplate[] initTemplates;
  private ServiceDeployment[] initDeployments;
  private ServiceRemoteInclusion[] initIncludes;

  protected ServiceConfiguration() {
  }

  public ServiceConfiguration(
    ServiceId serviceId,
    String runtime,
    boolean autoDeleteOnStop,
    boolean staticService,
    String[] groups,
    ServiceRemoteInclusion[] includes,
    ServiceTemplate[] templates,
    ServiceDeployment[] deployments,
    ProcessConfiguration processConfig,
    int port
  ) {
    this(
      serviceId,
      runtime,
      null,
      autoDeleteOnStop,
      staticService,
      port,
      processConfig,
      groups,
      new String[0],
      templates,
      deployments,
      includes,
      new ServiceTemplate[0],
      new ServiceDeployment[0],
      new ServiceRemoteInclusion[0],
      JsonDocument.newDocument());
  }

  public ServiceConfiguration(
    ServiceId serviceId,
    String runtime,
    boolean autoDeleteOnStop,
    boolean staticService,
    String[] groups,
    ServiceRemoteInclusion[] includes,
    ServiceTemplate[] templates,
    ServiceDeployment[] deployments,
    String[] deletedFilesAfterStop,
    ProcessConfiguration processConfig,
    int port
  ) {
    this(
      serviceId,
      runtime,
      null,
      autoDeleteOnStop,
      staticService,
      port,
      processConfig,
      groups,
      deletedFilesAfterStop,
      templates,
      deployments,
      includes,
      new ServiceTemplate[0],
      new ServiceDeployment[0],
      new ServiceRemoteInclusion[0],
      JsonDocument.newDocument());
  }

  public ServiceConfiguration(
    ServiceId serviceId,
    String runtime,
    boolean autoDeleteOnStop,
    boolean staticService,
    String[] groups,
    ServiceRemoteInclusion[] includes,
    ServiceTemplate[] templates,
    ServiceDeployment[] deployments,
    String[] deletedFilesAfterStop,
    ProcessConfiguration processConfig,
    int port,
    String javaCommand
  ) {
    this(
      serviceId,
      runtime,
      javaCommand,
      autoDeleteOnStop,
      staticService,
      port,
      processConfig,
      groups,
      deletedFilesAfterStop,
      templates,
      deployments,
      includes,
      new ServiceTemplate[0],
      new ServiceDeployment[0],
      new ServiceRemoteInclusion[0],
      JsonDocument.newDocument());
  }

  public ServiceConfiguration(
    ServiceId serviceId,
    String runtime,
    boolean autoDeleteOnStop,
    boolean staticService,
    String[] groups,
    ServiceRemoteInclusion[] includes,
    ServiceTemplate[] templates,
    ServiceDeployment[] deployments,
    ProcessConfiguration processConfig,
    JsonDocument properties,
    int port
  ) {
    this(
      serviceId,
      runtime,
      null,
      autoDeleteOnStop,
      staticService,
      port,
      processConfig,
      groups,
      new String[0],
      templates,
      deployments,
      includes,
      new ServiceTemplate[0],
      new ServiceDeployment[0],
      new ServiceRemoteInclusion[0],
      properties);
  }

  public ServiceConfiguration(
    ServiceId serviceId,
    String runtime,
    boolean autoDeleteOnStop,
    boolean staticService,
    String[] groups,
    ServiceRemoteInclusion[] includes,
    ServiceTemplate[] templates,
    ServiceDeployment[] deployments,
    ProcessConfiguration processConfig,
    JsonDocument properties,
    int port,
    String javaCommand
  ) {
    this(
      serviceId,
      runtime,
      javaCommand,
      autoDeleteOnStop,
      staticService,
      port,
      processConfig,
      groups,
      new String[0],
      templates,
      deployments,
      includes,
      new ServiceTemplate[0],
      new ServiceDeployment[0],
      new ServiceRemoteInclusion[0],
      properties);
  }

  public ServiceConfiguration(
    ServiceId serviceId,
    String runtime,
    boolean autoDeleteOnStop,
    boolean staticService,
    String[] groups,
    ServiceRemoteInclusion[] includes,
    ServiceTemplate[] templates,
    ServiceDeployment[] deployments,
    String[] deletedFilesAfterStop,
    ProcessConfiguration processConfig,
    JsonDocument properties,
    int port
  ) {
    this(
      serviceId,
      runtime,
      null,
      autoDeleteOnStop,
      staticService,
      port,
      processConfig,
      groups,
      deletedFilesAfterStop,
      templates,
      deployments,
      includes,
      new ServiceTemplate[0],
      new ServiceDeployment[0],
      new ServiceRemoteInclusion[0],
      properties);
  }

  public ServiceConfiguration(
    ServiceId serviceId,
    String runtime,
    String javaCommand,
    boolean autoDeleteOnStop,
    boolean staticService,
    int port,
    ProcessConfiguration processConfig,
    String[] groups,
    String[] deletedFilesAfterStop,
    ServiceTemplate[] templates,
    ServiceDeployment[] deployments,
    ServiceRemoteInclusion[] includes,
    ServiceTemplate[] initTemplates,
    ServiceDeployment[] initDeployments,
    ServiceRemoteInclusion[] initIncludes,
    JsonDocument properties
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
    this.initTemplates = initTemplates;
    this.initDeployments = initDeployments;
    this.initIncludes = initIncludes;
    this.properties = properties;
  }

  @NotNull
  public static Builder builder() {
    return new Builder();
  }

  @NotNull
  public static Builder builder(@NotNull ServiceTask task) {
    return builder().task(task);
  }

  @NotNull
  public ServiceId getServiceId() {
    return this.serviceId;
  }

  public void setServiceId(@NotNull ServiceId serviceId) {
    this.serviceId = serviceId;
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

  @Nullable
  public String getJavaCommand() {
    return this.javaCommand;
  }

  public void setJavaCommand(String javaCommand) {
    this.javaCommand = javaCommand;
  }

  @NotNull
  public String getRuntime() {
    return this.runtime;
  }

  public void setRuntime(@NotNull String runtime) {
    this.runtime = runtime;
  }

  @NotNull
  public String[] getGroups() {
    return this.groups;
  }

  public void setGroups(@NotNull String[] groups) {
    this.groups = groups;
  }

  public boolean hasGroup(@NotNull String group) {
    for (String s : this.groups) {
      if (s.equalsIgnoreCase(group)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  public ServiceRemoteInclusion[] getIncludes() {
    return this.includes;
  }

  public void setIncludes(@NotNull ServiceRemoteInclusion[] includes) {
    this.includes = includes;
  }

  @NotNull
  public ServiceTemplate[] getTemplates() {
    return this.templates;
  }

  public void setTemplates(@NotNull ServiceTemplate[] templates) {
    this.templates = templates;
  }

  @NotNull
  public ServiceDeployment[] getDeployments() {
    return this.deployments;
  }

  public void setDeployments(@NotNull ServiceDeployment[] deployments) {
    this.deployments = deployments;
  }

  @NotNull
  public String[] getDeletedFilesAfterStop() {
    return this.deletedFilesAfterStop;
  }

  public void setDeletedFilesAfterStop(@NotNull String[] deletedFilesAfterStop) {
    this.deletedFilesAfterStop = deletedFilesAfterStop;
  }

  public int getPort() {
    return this.port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public ServiceDeployment[] getInitDeployments() {
    return this.initDeployments;
  }

  public void setInitDeployments(ServiceDeployment[] initDeployments) {
    this.initDeployments = initDeployments;
  }

  public ServiceRemoteInclusion[] getInitIncludes() {
    return this.initIncludes;
  }

  public void setInitIncludes(ServiceRemoteInclusion[] initIncludes) {
    this.initIncludes = initIncludes;
  }

  public ServiceTemplate[] getInitTemplates() {
    return this.initTemplates;
  }

  public void setInitTemplates(ServiceTemplate[] initTemplates) {
    this.initTemplates = initTemplates;
  }

  @Nullable
  public ServiceInfoSnapshot createNewService() {
    return CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(this);
  }

  @NotNull
  public ITask<ServiceInfoSnapshot> createNewServiceAsync() {
    return CloudNetDriver.getInstance().getCloudServiceFactory().createCloudServiceAsync(this);
  }

  @NotNull
  public ProcessConfiguration getProcessConfig() {
    return this.processConfig;
  }

  public void setProcessConfig(@NotNull ProcessConfiguration processConfig) {
    this.processConfig = processConfig;
  }

  public boolean isValid() {
    return this.serviceId.taskName != null && this.serviceId.environment != null
      && this.processConfig.maxHeapMemorySize > 0 && this.port > 0;
  }

  public void replaceNulls() {
    if (this.templates == null) {
      this.templates = new ServiceTemplate[0];
    }
    if (this.deployments == null) {
      this.deployments = new ServiceDeployment[0];
    }
    if (this.includes == null) {
      this.includes = new ServiceRemoteInclusion[0];
    }
    if (this.serviceId.uniqueId == null) {
      this.serviceId.uniqueId = UUID.randomUUID();
    }
    if (this.processConfig.jvmOptions == null) {
      this.processConfig.jvmOptions = Collections.emptyList();
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
        .startPort(task.getStartPort())
        .javaCommand(task.getJavaCommand());
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
      this.config.groups = groups;
      return this;
    }

    /**
     * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
     * to the new service.
     */
    @NotNull
    public Builder groups(@NotNull Collection<String> groups) {
      return this.groups(groups.toArray(new String[0]));
    }

    /**
     * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
     * to the new service.
     */
    @NotNull
    public Builder addGroups(@NotNull String @NotNull ... groups) {
      List<String> groupList = new ArrayList<>(Arrays.asList(groups));
      groupList.addAll(Arrays.asList(this.config.groups));
      this.config.groups = groupList.toArray(new String[0]);
      return this;
    }

    /**
     * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
     * to the new service.
     */
    @NotNull
    public Builder addGroups(@NotNull Collection<String> groups) {
      return this.addGroups(groups.toArray(new String[0]));
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    @NotNull
    public Builder inclusions(@NotNull ServiceRemoteInclusion @NotNull ... inclusions) {
      this.config.includes = inclusions;
      return this;
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    @NotNull
    public Builder inclusions(@NotNull Collection<ServiceRemoteInclusion> inclusions) {
      return this.inclusions(inclusions.toArray(new ServiceRemoteInclusion[0]));
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    @NotNull
    public Builder addInclusions(@NotNull ServiceRemoteInclusion @NotNull ... inclusions) {
      List<ServiceRemoteInclusion> serviceRemoteInclusions = new ArrayList<>(Arrays.asList(inclusions));
      serviceRemoteInclusions.addAll(Arrays.asList(this.config.includes));
      this.config.includes = serviceRemoteInclusions.toArray(new ServiceRemoteInclusion[0]);
      return this;
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    @NotNull
    public Builder addInclusions(@NotNull Collection<ServiceRemoteInclusion> inclusions) {
      return this.addInclusions(inclusions.toArray(new ServiceRemoteInclusion[0]));
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    @NotNull
    public Builder templates(@NotNull ServiceTemplate @NotNull ... templates) {
      this.config.templates = templates;
      return this;
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    @NotNull
    public Builder templates(@NotNull Collection<ServiceTemplate> templates) {
      return this.templates(templates.toArray(new ServiceTemplate[0]));
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    @NotNull
    public Builder addTemplates(@NotNull ServiceTemplate @NotNull ... templates) {
      List<ServiceTemplate> serviceTemplates = new ArrayList<>(Arrays.asList(templates));
      serviceTemplates.addAll(Arrays.asList(this.config.templates));
      this.config.templates = serviceTemplates.toArray(new ServiceTemplate[0]);
      return this;
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    @NotNull
    public Builder addTemplates(@NotNull Collection<ServiceTemplate> templates) {
      return this.addTemplates(templates.toArray(new ServiceTemplate[0]));
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#deployResources()}.
     */
    @NotNull
    public Builder deployments(@NotNull ServiceDeployment @NotNull ... deployments) {
      this.config.deployments = deployments;
      return this;
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#deployResources()}.
     */
    @NotNull
    public Builder deployments(@NotNull Collection<ServiceDeployment> deployments) {
      return this.deployments(deployments.toArray(new ServiceDeployment[0]));
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#deployResources()}.
     */
    @NotNull
    public Builder addDeployments(@NotNull ServiceDeployment @NotNull ... deployments) {
      List<ServiceDeployment> serviceDeployments = new ArrayList<>(Arrays.asList(deployments));
      serviceDeployments.addAll(Arrays.asList(this.config.deployments));
      this.config.deployments = serviceDeployments.toArray(new ServiceDeployment[0]);
      return this;
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#deployResources()}.
     */
    @NotNull
    public Builder addDeployments(@NotNull Collection<ServiceDeployment> deployments) {
      return this.addDeployments(deployments.toArray(new ServiceDeployment[0]));
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    @NotNull
    public Builder deleteFilesAfterStop(@NotNull String @NotNull ... deletedFilesAfterStop) {
      this.config.deletedFilesAfterStop = deletedFilesAfterStop;
      return this;
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    @NotNull
    public Builder deleteFilesAfterStop(@NotNull Collection<String> deletedFilesAfterStop) {
      return this.deleteFilesAfterStop(deletedFilesAfterStop.toArray(new String[0]));
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    @NotNull
    public Builder addDeletedFilesAfterStop(@NotNull String @NotNull ... deletedFilesAfterStop) {
      List<String> deletedFiles = new ArrayList<>(Arrays.asList(deletedFilesAfterStop));
      deletedFiles.addAll(Arrays.asList(this.config.deletedFilesAfterStop));
      this.config.deletedFilesAfterStop = deletedFiles.toArray(new String[0]);
      return this;
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    @NotNull
    public Builder addDeletedFilesAfterStop(@NotNull Collection<String> deletedFilesAfterStop) {
      return this.addDeletedFilesAfterStop(deletedFilesAfterStop.toArray(new String[0]));
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
      this.config.processConfig.jvmOptions = new ArrayList<>(jvmOptions);
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
      this.config.processConfig.processParameters = new ArrayList<>(jvmOptions);
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
