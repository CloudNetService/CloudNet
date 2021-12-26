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
import de.dytanic.cloudnet.common.concurrent.Task;
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
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus.Internal;
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
    @NonNull ServiceId serviceId,
    @NonNull ProcessConfiguration processConfig,
    @NonNull String runtime,
    @Nullable String javaCommand,
    boolean autoDeleteOnStop,
    boolean staticService,
    @NonNull Set<String> groups,
    @NonNull Set<String> deletedFilesAfterStop,
    @NonNull Set<ServiceTemplate> templates,
    @NonNull Set<ServiceDeployment> deployments,
    @NonNull Set<ServiceRemoteInclusion> includes,
    int port,
    @NonNull JsonDocument properties
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

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ServiceTask task) {
    return builder().task(task);
  }

  public @NonNull ServiceId serviceId() {
    return this.serviceId;
  }

  public boolean autoDeleteOnStop() {
    return this.autoDeleteOnStop;
  }

  public boolean staticService() {
    return this.staticService;
  }

  public @Nullable String javaCommand() {
    return this.javaCommand;
  }

  public @NonNull String runtime() {
    return this.runtime;
  }

  public @NonNull Set<String> groups() {
    return this.groups;
  }

  public @NonNull Set<String> deletedFilesAfterStop() {
    return this.deletedFilesAfterStop;
  }

  public @NonNull Set<ServiceTemplate> templates() {
    return this.templates;
  }

  public @NonNull Set<ServiceDeployment> deployments() {
    return this.deployments;
  }

  public @NonNull Set<ServiceRemoteInclusion> includes() {
    return this.includes;
  }

  public @NonNull ProcessConfiguration processConfig() {
    return this.processConfig;
  }

  public @Range(from = 0, to = 65535) int port() {
    return this.port;
  }

  @Internal
  public void port(@Range(from = 0, to = 65535) int port) {
    this.port = port;
  }

  public @Nullable ServiceInfoSnapshot createNewService() {
    return CloudNetDriver.instance().cloudServiceFactory().createCloudService(this);
  }

  public @NonNull Task<ServiceInfoSnapshot> createNewServiceAsync() {
    return CloudNetDriver.instance().cloudServiceFactory().createCloudServiceAsync(this);
  }

  @Override
  public @NonNull ServiceConfiguration clone() {
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
    public @NonNull Builder task(@NonNull ServiceTask task) {
      return this
        .task(task.name())

        .runtime(task.runtime())
        .javaCommand(task.javaCommand())
        .nameSplitter(task.nameSplitter())

        .autoDeleteOnStop(task.autoDeleteOnStop())
        .staticService(task.staticServices())

        .allowedNodes(task.associatedNodes())
        .groups(task.groups())
        .deleteFilesAfterStop(task.deletedFilesAfterStop())

        .templates(task.templates())
        .deployments(task.deployments())
        .inclusions(task.includes())

        .environment(task.processConfiguration().environment())
        .maxHeapMemory(task.processConfiguration().maxHeapMemorySize())
        .jvmOptions(task.processConfiguration().jvmOptions())
        .processParameters(task.processConfiguration().processParameters())
        .startPort(task.startPort());
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
    public @NonNull Builder serviceId(@NonNull ServiceId.Builder serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * The task for the new service. No permanent task with that name has to exist. This will NOT use any options of the
     * given task, to do that use {@link #task(ServiceTask)}.
     */
    public @NonNull Builder task(@NonNull String task) {
      this.serviceId.taskName(task);
      return this;
    }

    public @NonNull Builder environment(@NonNull String environment) {
      this.serviceId.environment(environment);
      this.processConfig.environment(environment);
      return this;
    }

    /**
     * The environment for the new service.
     */
    public @NonNull Builder environment(@NonNull ServiceEnvironmentType environment) {
      this.serviceId.environment(environment);
      this.processConfig.environment(environment);
      return this;
    }

    /**
     * The task id for the new service (For example Lobby-1 would have the task id 1).
     */
    public @NonNull Builder taskId(int taskId) {
      this.serviceId.taskServiceId(taskId);
      return this;
    }

    /**
     * The uniqueId for the new service.
     */
    public @NonNull Builder uniqueId(@NonNull UUID uniqueId) {
      this.serviceId.uniqueId(uniqueId);
      return this;
    }

    /**
     * the java command to start the service with if this is null the default value from the config.json will be used
     */
    public @NonNull Builder javaCommand(@Nullable String javaCommand) {
      this.javaCommand = javaCommand;
      return this;
    }

    public @NonNull Builder nameSplitter(@NonNull String nameSplitter) {
      this.serviceId.nameSplitter(nameSplitter);
      return this;
    }

    /**
     * The node where the new service will start. If the service cannot be created on this node or the node doesn't
     * exist, it will NOT be created and {@link ServiceConfiguration#createNewService()} will return {@code null}.
     */
    public @NonNull Builder node(@Nullable String nodeUniqueId) {
      this.serviceId.nodeUniqueId(nodeUniqueId);
      return this;
    }

    public @NonNull Builder allowedNodes(String @NonNull ... allowedNodes) {
      return this.allowedNodes(Arrays.asList(allowedNodes));
    }

    /**
     * A list of all allowed nodes. CloudNet will choose the node with the most free resources. If a node is provided
     * using {@link #node(String)}, this option will be ignored.
     */
    public @NonNull Builder allowedNodes(@NonNull Collection<String> allowedNodes) {
      this.serviceId.allowedNodes(allowedNodes);
      return this;
    }

    /**
     * The runtime of the service. If none is provided, the default "jvm" is used. By default, CloudNet only provides
     * the "jvm" runtime, you can add your own with custom modules.
     */
    public @NonNull Builder runtime(@NonNull String runtime) {
      this.runtime = runtime;
      return this;
    }

    /**
     * Whether this service should be deleted on stop (doesn't affect files of a static service) or the life cycle
     * should be changed to {@link ServiceLifeCycle#PREPARED}.
     */
    public @NonNull Builder autoDeleteOnStop(boolean autoDeleteOnStop) {
      this.autoDeleteOnStop = autoDeleteOnStop;
      return this;
    }

    /**
     * Alias for {@code autoDeleteOnStop(true)}.
     */
    public @NonNull Builder autoDeleteOnStop() {
      return this.autoDeleteOnStop(true);
    }

    /**
     * Whether the files should be deleted or saved on deletion of the service.
     */
    public @NonNull Builder staticService(boolean staticService) {
      this.staticService = staticService;
      return this;
    }

    /**
     * Alias for {@code staticService(true)}.
     */
    public @NonNull Builder staticService() {
      return this.staticService(true);
    }

    /**
     * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
     * to the new service.
     */
    public @NonNull Builder groups(@NonNull String @NonNull ... groups) {
      return this.groups(Arrays.asList(groups));
    }

    /**
     * The groups for the new service. CloudNet will apply every template, deployment and inclusion of the given groups
     * to the new service.
     */
    public @NonNull Builder groups(@NonNull Collection<String> groups) {
      this.groups = new HashSet<>(groups);
      return this;
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    public @NonNull Builder inclusions(ServiceRemoteInclusion @NonNull ... inclusions) {
      return this.inclusions(Arrays.asList(inclusions));
    }

    /**
     * The inclusions for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceInclusions()}.
     */
    public @NonNull Builder inclusions(@NonNull Collection<ServiceRemoteInclusion> inclusions) {
      this.includes = new HashSet<>(inclusions);
      return this;
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    public @NonNull Builder templates(ServiceTemplate @NonNull ... templates) {
      return this.templates(Arrays.asList(templates));
    }

    /**
     * The templates for the new service. They will be copied into the service directory before the service is started
     * or by calling {@link SpecificCloudServiceProvider#includeWaitingServiceTemplates()}.
     */
    public @NonNull Builder templates(@NonNull Collection<ServiceTemplate> templates) {
      this.templates = new HashSet<>(templates);
      return this;
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#removeAndExecuteDeployments()}.
     */
    public @NonNull Builder deployments(ServiceDeployment @NonNull ... deployments) {
      return this.deployments(Arrays.asList(deployments));
    }

    /**
     * The deployments for the new service. They will be copied into the template after the service is stopped or by
     * calling {@link SpecificCloudServiceProvider#removeAndExecuteDeployments()}.
     */
    public @NonNull Builder deployments(@NonNull Collection<ServiceDeployment> deployments) {
      this.deployments = new HashSet<>(deployments);
      return this;
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    public @NonNull Builder deleteFilesAfterStop(String @NonNull ... deletedFilesAfterStop) {
      return this.deleteFilesAfterStop(Arrays.asList(deletedFilesAfterStop));
    }

    /**
     * The files that should be deleted after the service has been stopped.
     */
    public @NonNull Builder deleteFilesAfterStop(@NonNull Collection<String> deletedFilesAfterStop) {
      this.deletedFilesAfterStop = new HashSet<>(deletedFilesAfterStop);
      return this;
    }

    /**
     * The max heap memory for the new service.
     */
    public @NonNull Builder maxHeapMemory(int maxHeapMemory) {
      this.processConfig.maxHeapMemorySize(maxHeapMemory);
      return this;
    }

    /**
     * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup
     * command.
     */
    public @NonNull Builder jvmOptions(@NonNull Collection<String> jvmOptions) {
      this.processConfig.jvmOptions(jvmOptions);
      return this;
    }

    /**
     * The jvm options for the new service. They will be added directly before the "-Xmx" parameter in the startup
     * command.
     */
    public @NonNull Builder jvmOptions(@NonNull String @NonNull ... jvmOptions) {
      return this.jvmOptions(Arrays.asList(jvmOptions));
    }

    public @NonNull Builder processParameters(String @NonNull ... processParameters) {
      return this.processParameters(Arrays.asList(processParameters));
    }

    /**
     * The process parameters for the new service. This will be the last parameters that will be added to the command.
     */
    public @NonNull Builder processParameters(@NonNull Collection<String> processParameters) {
      this.processConfig.processParameters(processParameters);
      return this;
    }

    /**
     * The start port for the new service. CloudNet will test whether the port is used or not, it will count up 1 while
     * the port is used.
     */
    public @NonNull Builder startPort(int startPort) {
      this.port = startPort;
      return this;
    }

    /**
     * The default properties of the new service. CloudNet itself completely ignores them, but they can be useful if you
     * want to transport data from the component that has created the service to the new service.
     */
    public @NonNull Builder properties(@NonNull JsonDocument properties) {
      this.properties = properties;
      return this;
    }

    public @NonNull ServiceConfiguration build() {
      Verify.verify(this.port > 0 && this.port <= 65535, "invalid port provided");
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
