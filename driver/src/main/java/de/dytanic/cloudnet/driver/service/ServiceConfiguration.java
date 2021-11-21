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
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceConfiguration extends JsonDocPropertyHolder implements Cloneable {

  protected final ServiceId serviceId;
  protected final ProcessConfiguration processConfig;

  protected final String runtime;
  protected final String javaCommand;

  protected final boolean autoDeleteOnStop;
  protected final boolean staticService;

  protected final Set<String> groups;
  protected final Set<String> deletedFilesAfterStop;

  protected final Set<ServiceTemplate> templates;
  protected final Set<ServiceDeployment> deployments;
  protected final Set<ServiceRemoteInclusion> includes;

  protected volatile int port;

  protected ServiceConfiguration(
    @NotNull ServiceId serviceId,
    @NotNull ProcessConfiguration processConfig,
    @NotNull String runtime,
    @Nullable String javaCommand,
    boolean autoDeleteOnStop,
    boolean staticService,
    @NotNull Set<String> groups,
    @NotNull Set<String> deletedFilesAfterStop,
    @NotNull Set<ServiceTemplate> templates,
    @NotNull Set<ServiceDeployment> deployments,
    @NotNull Set<ServiceRemoteInclusion> includes,
    int port,
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

  public boolean isStaticService() {
    return this.staticService;
  }

  public @Nullable String getJavaCommand() {
    return this.javaCommand;
  }

  public @NotNull String getRuntime() {
    return this.runtime;
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

  @Internal
  public void setPort(@Range(from = 0, to = 65535) int port) {
    this.port = port;
  }

  public @Nullable ServiceInfoSnapshot createNewService() {
    return CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(this);
  }

  public @NotNull ITask<ServiceInfoSnapshot> createNewServiceAsync() {
    return CloudNetDriver.getInstance().getCloudServiceFactory().createCloudServiceAsync(this);
  }

  @Override
  public @NotNull ServiceConfiguration clone() {
    try {
      return (ServiceConfiguration) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
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

    protected ServiceId.Builder serviceId = ServiceId.builder();
    protected ProcessConfiguration.Builder processConfig = ProcessConfiguration.builder();

    protected String javaCommand;
    protected String runtime = "jvm";

    protected boolean staticService;
    protected boolean autoDeleteOnStop = true;

    protected int port = 44955;
    protected JsonDocument properties = JsonDocument.newDocument();

    protected Set<String> groups = new HashSet<>();
    protected Set<String> deletedFilesAfterStop = new HashSet<>();

    protected Set<ServiceTemplate> templates = new HashSet<>();
    protected Set<ServiceDeployment> deployments = new HashSet<>();
    protected Set<ServiceRemoteInclusion> includes = new HashSet<>();

    /**
     * Applies every option of the given {@link ServiceTask} object except for the Properties. This will override every
     * previously set option of this builder.
     */
    public @NotNull Builder task(@NotNull ServiceTask task) {
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
    public @NotNull Builder serviceId(@NotNull ServiceId.Builder serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * The task for the new service. No permanent task with that name has to exist. This will NOT use any options of the
     * given task, to do that use {@link #task(ServiceTask)}.
     */
    public @NotNull Builder task(@NotNull String task) {
      this.serviceId.taskName(task);
      return this;
    }

    public @NotNull Builder environment(@NotNull String environment) {
      this.serviceId.environment(environment);
      this.processConfig.environment(environment);
      return this;
    }

    /**
     * The environment for the new service.
     */
    public @NotNull Builder environment(@NotNull ServiceEnvironmentType environment) {
      this.serviceId.environment(environment);
      this.processConfig.environment(environment);
      return this;
    }

    /**
     * The task id for the new service (For example Lobby-1 would have the task id 1).
     */
    public @NotNull Builder taskId(int taskId) {
      this.serviceId.taskServiceId(taskId);
      return this;
    }

    /**
     * The uniqueId for the new service.
     */
    public @NotNull Builder uniqueId(@NotNull UUID uniqueId) {
      this.serviceId.uniqueId(uniqueId);
      return this;
    }

    /**
     * the java command to start the service with if this is null the default value from the config.json will be used
     */
    public @NotNull Builder javaCommand(@Nullable String javaCommand) {
      this.javaCommand = javaCommand;
      return this;
    }

    public @NotNull Builder nameSplitter(@NotNull String nameSplitter) {
      this.serviceId.nameSplitter(nameSplitter);
      return this;
    }

    /**
     * The node where the new service will start. If the service cannot be created on this node or the node doesn't
     * exist, it will NOT be created and {@link ServiceConfiguration#createNewService()} will return {@code null}.
     */
    public @NotNull Builder node(@NotNull String nodeUniqueId) {
      this.serviceId.nodeUniqueId(nodeUniqueId);
      return this;
    }

    public @NotNull Builder allowedNodes(String @NotNull ... allowedNodes) {
      return this.allowedNodes(Arrays.asList(allowedNodes));
    }

    /**
     * A list of all allowed nodes. CloudNet will choose the node with the most free resources. If a node is provided
     * using {@link #node(String)}, this option will be ignored.
     */
    public @NotNull Builder allowedNodes(@NotNull Collection<String> allowedNodes) {
      this.serviceId.allowedNodes(allowedNodes);
      return this;
    }

    /**
     * The runtime of the service. If none is provided, the default "jvm" is used. By default, CloudNet only provides
     * the "jvm" runtime, you can add your own with custom modules.
     */
    public @NotNull Builder runtime(@NotNull String runtime) {
      this.runtime = runtime;
      return this;
    }

    /**
     * Whether this service should be deleted on stop (doesn't affect files of a static service) or the life cycle
     * should be changed to {@link ServiceLifeCycle#PREPARED}.
     */
    public @NotNull Builder autoDeleteOnStop(boolean autoDeleteOnStop) {
      this.autoDeleteOnStop = autoDeleteOnStop;
      return this;
    }

    /**
     * Alias for {@code autoDeleteOnStop(true)}.
     */
    public @NotNull Builder autoDeleteOnStop() {
      return this.autoDeleteOnStop(true);
    }

    /**
     * Whether the files should be deleted or saved on deletion of the service.
     */
    public @NotNull Builder staticService(boolean staticService) {
      this.staticService = staticService;
      return this;
    }

    /**
     * Alias for {@code staticService(true)}.
     */
    public @NotNull Builder staticService() {
      return this.staticService(true);
    }

    /**
     * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
     * to the new service.
     */
    public @NotNull Builder groups(@NotNull String @NotNull ... groups) {
      return this.groups(Arrays.asList(groups));
    }

    /**
     * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
     * to the new service.
     */
    public @NotNull Builder groups(@NotNull Collection<String> groups) {
      this.groups = new HashSet<>(groups);
      return this;
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    public @NotNull Builder inclusions(ServiceRemoteInclusion @NotNull ... inclusions) {
      return this.inclusions(Arrays.asList(inclusions));
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    public @NotNull Builder inclusions(@NotNull Collection<ServiceRemoteInclusion> inclusions) {
      this.includes = new HashSet<>(inclusions);
      return this;
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    public @NotNull Builder templates(ServiceTemplate @NotNull ... templates) {
      return this.templates(Arrays.asList(templates));
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    public @NotNull Builder templates(@NotNull Collection<ServiceTemplate> templates) {
      this.templates = new HashSet<>(templates);
      return this;
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#removeAndExecuteDeployments()}.
     */
    public @NotNull Builder deployments(ServiceDeployment @NotNull ... deployments) {
      return this.deployments(Arrays.asList(deployments));
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#removeAndExecuteDeployments()}.
     */
    public @NotNull Builder deployments(@NotNull Collection<ServiceDeployment> deployments) {
      this.deployments = new HashSet<>(deployments);
      return this;
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    public @NotNull Builder deleteFilesAfterStop(String @NotNull ... deletedFilesAfterStop) {
      return this.deleteFilesAfterStop(Arrays.asList(deletedFilesAfterStop));
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    public @NotNull Builder deleteFilesAfterStop(@NotNull Collection<String> deletedFilesAfterStop) {
      this.deletedFilesAfterStop = new HashSet<>(deletedFilesAfterStop);
      return this;
    }

    /**
     * The max heap memory for the new service.
     */
    public @NotNull Builder maxHeapMemory(int maxHeapMemory) {
      this.processConfig.maxHeapMemorySize(maxHeapMemory);
      return this;
    }

    /**
     * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup
     * command.
     */
    public @NotNull Builder jvmOptions(@NotNull Collection<String> jvmOptions) {
      this.processConfig.jvmOptions(jvmOptions);
      return this;
    }

    /**
     * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup
     * command.
     */
    public @NotNull Builder jvmOptions(@NotNull String @NotNull ... jvmOptions) {
      return this.jvmOptions(Arrays.asList(jvmOptions));
    }

    public @NotNull Builder processParameters(String @NotNull ... processParameters) {
      return this.processParameters(Arrays.asList(processParameters));
    }

    /**
     * The process parameters for the new service. This will be the last parameters that will be added to the command.
     */
    public @NotNull Builder processParameters(@NotNull Collection<String> processParameters) {
      this.processConfig.processParameters(processParameters);
      return this;
    }

    /**
     * The start port for the new service. CloudNet will test whether the port is used or not, it will count up 1 while
     * the port is used.
     */
    public @NotNull Builder startPort(int startPort) {
      this.port = startPort;
      return this;
    }

    /**
     * The default properties of the new service. CloudNet itself completely ignores them, but they can be useful if you
     * want to transport data from the component that has created the service to the new service.
     */
    public @NotNull Builder properties(@NotNull JsonDocument properties) {
      this.properties = properties;
      return this;
    }

    public @NotNull ServiceConfiguration build() {
      Verify.verifyNotNull(this.port > 0 && this.port <= 65535, "invalid port provided");
      return new ServiceConfiguration(
        this.serviceId.build(),
        this.processConfig.build(),
        this.runtime,
        this.javaCommand,
        this.autoDeleteOnStop,
        this.staticService,
        this.groups,
        this.deletedFilesAfterStop,
        this.templates,
        this.deployments,
        this.includes,
        this.port,
        this.properties);
    }
  }
}
