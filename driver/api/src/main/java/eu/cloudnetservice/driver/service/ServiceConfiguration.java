/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.service;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The configuration based on which a service gets created. This configuration is completely lose from any service task
 * or group configuration and only softly includes them. This means that creating a service without having to create a
 * task first is completely possible and supported. One example use case are private servers which don't require a task
 * to be created.
 * <p>
 * Example usage to create a service based on a task:
 * <pre>
 * {@code
 *  public final class ServiceCreateHelper {
 *    public void createService() {
 *      ServiceTask task = serviceTaskProvider.serviceTask("Lobby");
 *      ServiceConfiguration config = ServiceConfiguration.builder(task).build();
 *      ServiceCreateResult createResult = config.createNewService();
 *
 *      if (createResult.state() == ServiceCreateResult.State.CREATED) {
 *        // the service was created
 *        // prints for example "Lobby-1"
 *        System.out.println(createResult.serviceInfo().name());
 *      } else {
 *        // service creation failed or was deferred
 *        System.out.println("Unable to create service: " + createResult.state());
 *      }
 *    }
 *  }
 * }
 * </pre>
 * <p>
 * Example of changing the task name of the service, this change will show up in the service name:
 * <pre>
 * {@code
 *  public final class ServiceCreateHelper {
 *    public void createService() {
 *      ServiceTask task = serviceTaskProvider.serviceTask("Lobby");
 *      ServiceConfiguration config = ServiceConfiguration.builder(task)
 *        .taskName("HelloWorld")
 *        .build();
 *      ServiceCreateResult createResult = config.createNewService();
 *
 *      if (createResult.state() == ServiceCreateResult.State.CREATED) {
 *        // the service was created
 *        // prints for example "HelloWorld-1" because we changed the name of the task
 *        System.out.println(createResult.serviceInfo().name());
 *      } else {
 *        // service creation failed or was deferred
 *        // for example not enough heap memory was free to start the service
 *        System.out.println("Unable to create service: " + createResult.state());
 *      }
 *    }
 *  }
 * }
 * </pre>
 * <p>
 * But you can also create a service without any task, configured as we want it to be. It's required to set the task and
 * environment name to create a service that way. Example:
 * <pre>
 * {@code
 *  public final class ServiceCreateHelper {
 *    public void createService() {
 *      ServiceConfiguration config = ServiceConfiguration.builder()
 *        .nameSplitter("#")
 *        .maxHeapMemory(1024)
 *        .taskName("HelloWorld")
 *        .environment("MINECRAFT_SERVER")
 *        .build();
 *      ServiceCreateResult createResult = config.createNewService();
 *
 *      if (createResult.state() == ServiceCreateResult.State.CREATED) {
 *        // the service was created
 *        // prints for example "HelloWorld#1" because
 *        // we set the task name and name splitter to these values
 *        System.out.println(createResult.serviceInfo().name());
 *      } else {
 *        // service creation failed or was deferred
 *        // for example not enough heap memory was free to start the service
 *        System.out.println("Unable to create service: " + createResult.state());
 *      }
 *    }
 *  }
 * }
 * </pre>
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceConfiguration extends ServiceConfigurationBase implements Cloneable {

  protected final ServiceId serviceId;
  protected final ProcessConfiguration processConfig;
  protected final ServiceCreateRetryConfiguration retryConfiguration;

  protected final int port;
  protected final String runtime;
  protected final String hostAddress;
  protected final String javaCommand;

  protected final boolean autoDeleteOnStop;
  protected final boolean staticService;

  protected final Set<String> groups;
  protected final Set<String> deletedFilesAfterStop;

  /**
   * Constructs a new service configuration instance.
   *
   * @param serviceId             the id of the service to create based on the configuration.
   * @param processConfig         the process configuration of the service which gets created.
   * @param retryConfiguration    the service create retry configuration to apply if the initial service create fails.
   * @param port                  the port of the service to start with, might get increased when already taken.
   * @param runtime               the runtime of the service to create it based on, for example {@code jvm}.
   * @param hostAddress           the host address the service based on this configuration is bound to.
   * @param javaCommand           the java command to use when starting a service.
   * @param autoDeleteOnStop      if the service should get deleted when stopping.
   * @param staticService         if the service which gets created should be static (no file deletion when stopping).
   * @param groups                the names of the group configurations to include before starting the service.
   * @param deletedFilesAfterStop all files which should get deleted when stopping a service based on this config.
   * @param templates             the templates to include before starting a service based on this config.
   * @param deployments           the deployments to execute when stopping a service based on this config.
   * @param includes              the inclusions to include before starting a service based on this configuration.
   * @param properties            the properties which should get copied onto the service before starting.
   * @throws NullPointerException if one of the given parameters is null.
   */
  protected ServiceConfiguration(
    @NonNull ServiceId serviceId,
    @NonNull ProcessConfiguration processConfig,
    @NonNull ServiceCreateRetryConfiguration retryConfiguration,
    int port,
    @NonNull String runtime,
    @Nullable String hostAddress,
    @Nullable String javaCommand,
    boolean autoDeleteOnStop,
    boolean staticService,
    @NonNull Set<String> groups,
    @NonNull Set<String> deletedFilesAfterStop,
    @NonNull Set<ServiceTemplate> templates,
    @NonNull Set<ServiceDeployment> deployments,
    @NonNull Set<ServiceRemoteInclusion> includes,
    @NonNull Document properties
  ) {
    super(templates, deployments, includes, properties);

    this.serviceId = serviceId;
    this.port = port;
    this.runtime = runtime;
    this.hostAddress = hostAddress;
    this.javaCommand = javaCommand;
    this.autoDeleteOnStop = autoDeleteOnStop;
    this.staticService = staticService;
    this.processConfig = processConfig;
    this.groups = groups;
    this.retryConfiguration = retryConfiguration;
    this.deletedFilesAfterStop = deletedFilesAfterStop;
  }

  /**
   * Constructs a new builder instance for a service configuration.
   *
   * @return a new service configuration builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new builder instance for a service configuration which has the same options set as the given service
   * task. Note: the properties of the task will not get copied into the configuration as they don't explicitly belong
   * to the service. If you do want the properties in the task anyway, make sure to set them manually in the builder.
   * <p>
   * Changes made to the given service task will not reflect into the created builder and vice-versa.
   *
   * @param task the task to copy the options of.
   * @return a new builder instance initialized with the options set in the given task.
   * @throws NullPointerException if the given task is null.
   */
  public static @NonNull Builder builder(@NonNull ServiceTask task) {
    return builder()
      .taskName(task.name())

      .runtime(task.runtime())
      .hostAddress(task.hostAddress())
      .javaCommand(task.javaCommand())
      .nameSplitter(task.nameSplitter())

      .autoDeleteOnStop(task.autoDeleteOnStop())
      .staticService(task.staticServices())

      .allowedNodes(task.associatedNodes())
      .groups(task.groups())
      .deletedFilesAfterStop(task.deletedFilesAfterStop())

      .templates(task.templates())
      .deployments(task.deployments())
      .inclusions(task.inclusions())

      .jvmOptions(task.processConfiguration().jvmOptions())
      .processParameters(task.processConfiguration().processParameters())
      .environmentVariables(task.processConfiguration().environmentVariables())

      .environment(task.processConfiguration().environment())
      .maxHeapMemory(task.processConfiguration().maxHeapMemorySize())
      .startPort(task.startPort());
  }

  /**
   * Creates a new service configuration builder which has the same options set as the given service configuration.
   * Changes made to the given configuration will not reflect into the new builder and vice-versa.
   * <p>
   * When calling build directly after creating the builder based on the given service configuration it will return a
   * service configuration which is equal to the given one, but not identical.
   *
   * @param configuration the configuration to copy the set options of.
   * @return a new builder instance which has the same options set as the given configuration.
   * @throws NullPointerException if the given configuration is null.
   */
  public static @NonNull Builder builder(@NonNull ServiceConfiguration configuration) {
    return builder()
      .serviceId(ServiceId.builder(configuration.serviceId()))
      .runtime(configuration.runtime())
      .hostAddress(configuration.hostAddress())
      .javaCommand(configuration.javaCommand())
      .autoDeleteOnStop(configuration.autoDeleteOnStop())
      .staticService(configuration.staticService())
      .startPort(configuration.port())
      .processConfig(ProcessConfiguration.builder(configuration.processConfig()))
      .groups(configuration.groups())
      .deletedFilesAfterStop(configuration.deletedFilesAfterStop())
      .templates(configuration.templates())
      .deployments(configuration.deployments())
      .inclusions(configuration.inclusions())
      .properties(configuration.propertyHolder())
      .retryConfiguration(configuration.retryConfiguration());
  }

  /**
   * Get the base service id all services created based on this configuration will use. However, pre-create checks might
   * change the configuration if needed, for example increasing the service id if it is already taken or setting the
   * node on which the service will start when it is not set already. These changes will not reflect into the service id
   * object returned by this object.
   *
   * @return the base service id for services created using this configuration.
   */
  public @NonNull ServiceId serviceId() {
    return this.serviceId;
  }

  /**
   * Get the creation retry configuration that will be applied to all services which are created based on the
   * configuration. If the configuration is enabled and a service cannot get created, then the configuration will be
   * used to retry the service creation.
   *
   * @return the retry configuration for services created using this configuration.
   */
  public @NonNull ServiceCreateRetryConfiguration retryConfiguration() {
    return this.retryConfiguration;
  }

  /**
   * Get if services should be deleted when stopping the service. This does only mean that the service gets unregistered
   * and is no longer available for starting, but does not mean that all service files get deleted when the service is
   * static.
   * <p>
   * If this option is false the service will be stopped and then go back to the prepared state, ready to get started
   * again.
   *
   * @return if the service should get unregistered when stopping it.
   */
  public boolean autoDeleteOnStop() {
    return this.autoDeleteOnStop;
  }

  /**
   * Get if services which get created based on this configuration are static. Files of static services will not get
   * deleted when deleting the service. That means that starting the exact service after it was deleted, will result in
   * the same service files to be present.
   * <p>
   * <strong>NOTE:</strong> static services are not automatically synced between nodes, therefore a specific node to
   * start the service on should be present, else it might result in a different configuration (or world) than
   * expected.
   *
   * @return if services created based on this configuration should be static.
   */
  public boolean staticService() {
    return this.staticService;
  }

  /**
   * Get the java command to use when starting a service. This can for example be used to use different java distros or
   * versions on services. If no java command is set the java command configured in the node that is picking up the
   * service will be used.
   *
   * @return the java command to use when starting services based on this configuration.
   */
  public @Nullable String javaCommand() {
    return this.javaCommand;
  }

  /**
   * Get the runtime to use when services gets created based on this configuration. Runtimes are there to allow
   * different types of configuration for running in different environments, for example inside a docker container. The
   * given runtime must be registered on the node which picks up the service, if not it will result in an error.
   *
   * @return the runtime to use when creating a service based on this configuration.
   */
  public @NonNull String runtime() {
    return this.runtime;
  }

  /**
   * Get the host address that all services based on this configuration are bound to. The host address is not required
   * to be an ip address, it is possible that the host address is just an ip alias which needs to be resolved using the
   * configuration of the node. The host address might be null, in that case the fallback host of the node is used.
   * <p>
   * Note: Might be null until the host address was resolved during the preparation of a service based on this
   * configuration. The resolved host address is not reflected into the original configuration, only into the
   * configuration which is available through the service information snapshot.
   *
   * @return the host address that all services based on this configuration are bound to, null if not resolved yet.
   */
  public @UnknownNullability String hostAddress() {
    return this.hostAddress;
  }

  /**
   * Get the names of the groups whose configuration should get included when starting a service based on this
   * configuration. Each configuration is only included when it is present on the node starting the service, if not it
   * will silently be ignored. Note that groups targeting the same environment as this configuration will get included
   * automatically.
   *
   * @return the names of the group configurations to include their configurations before starting.
   */
  @Unmodifiable
  public @NonNull Set<String> groups() {
    return this.groups;
  }

  /**
   * Get the files (or directories) which should get deleted when stopping the service (before deployments are
   * executed). Trying to delete files outside the service directory will result in an exception.
   *
   * @return a set of path to files/directories which should get deleted when stopping a service based on this config.
   */
  @Unmodifiable
  public @NonNull Set<String> deletedFilesAfterStop() {
    return this.deletedFilesAfterStop;
  }

  /**
   * Get the process configuration to apply to all services which get created based on this configuration.
   *
   * @return the process configuration to apply to all services.
   */
  public @NonNull ProcessConfiguration processConfig() {
    return this.processConfig;
  }

  /**
   * The port of the service to start on. If the given port is already taken it will be counted up until it reaches the
   * port limit (65535) or one of the ports in between is free to be taken. Port changes because of counting up will not
   * be reflected into this configuration and vice-versa.
   *
   * @return the port number to start the service on, might be counted up when the port is already taken.
   */
  public @Range(from = 0, to = 0xFFFF) int port() {
    return this.port;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Unmodifiable
  public @NonNull Collection<String> jvmOptions() {
    return this.processConfig.jvmOptions();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Unmodifiable
  public @NonNull Collection<String> processParameters() {
    return this.processConfig.processParameters();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Unmodifiable
  public @NonNull Map<String, String> environmentVariables() {
    return this.processConfig.environmentVariables();
  }

  /**
   * Creates and prepares a service based on this configuration using the default cloud service factory.
   *
   * @return a result representing the state of the service creation.
   * @see CloudServiceFactory#createCloudService(ServiceConfiguration)
   */
  public @NonNull ServiceCreateResult createNewService() {
    var serviceFactory = InjectionLayer.boot().instance(CloudServiceFactory.class);
    return serviceFactory.createCloudService(this);
  }

  /**
   * Creates and prepares a service based on this configuration using the default cloud service factory.
   *
   * @return a task completed with a result representing the state of the service creation.
   * @see CloudServiceFactory#createCloudServiceAsync(ServiceConfiguration)
   */
  public @NonNull CompletableFuture<ServiceCreateResult> createNewServiceAsync() {
    var serviceFactory = InjectionLayer.boot().instance(CloudServiceFactory.class);
    return serviceFactory.createCloudServiceAsync(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceConfiguration clone() {
    try {
      return (ServiceConfiguration) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  /**
   * Represents a builder for a service configuration.
   *
   * @since 4.0
   */
  public static class Builder extends ServiceConfigurationBase.Builder<ServiceConfiguration, Builder> {

    protected ServiceId.Builder serviceId = ServiceId.builder();
    protected ProcessConfiguration.Builder processConfig = ProcessConfiguration.builder();
    protected ServiceCreateRetryConfiguration retryConfiguration = ServiceCreateRetryConfiguration.NO_RETRY;

    protected String javaCommand;
    protected String hostAddress;
    protected String runtime = "jvm";

    protected boolean staticService;
    protected boolean autoDeleteOnStop = true;

    protected int port = 44955;

    protected Set<String> groups = new HashSet<>();
    protected Set<String> deletedFilesAfterStop = new HashSet<>();

    /**
     * Sets the service id builder of this builder. Further calls might overwrite changes in the given builder, for
     * example when setting the environment of the configuration via this builder.
     *
     * @param serviceId the new service id builder to use.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given service id builder is null.
     */
    public @NonNull Builder serviceId(@NonNull ServiceId.Builder serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * Sets the process configuration builder of this builder. Further calls might overwrite changes in the given
     * builder, for example when setting the max heap memory services are allowed to use.
     *
     * @param processConfig the new process configuration to use.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given process configuration builder is null.
     */
    public @NonNull Builder processConfig(@NonNull ProcessConfiguration.Builder processConfig) {
      this.processConfig = processConfig;
      return this;
    }

    /**
     * Sets the creation retry configuration that will be applied to all services which are created based on the
     * configuration. If the configuration is enabled and a service cannot get created, then the configuration will be
     * used to retry the service creation.
     *
     * @param retryConfiguration the retry configuration to use.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given retry configuration is null.
     */
    public @NonNull Builder retryConfiguration(@NonNull ServiceCreateRetryConfiguration retryConfiguration) {
      this.retryConfiguration = retryConfiguration;
      return this;
    }

    /**
     * Sets the task name to use for the created services. This method will not change any other option than the task
     * name, so if a task with the given name exists it has no effect when calling this method as nothing will be copied
     * from that task into this builder.
     * <p>
     * <strong>NOTE:</strong> the given task name must still match the defined pattern for a task name.
     *
     * @param taskName a task name, no task with that name must exist.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException     if the given task name is null.
     * @throws IllegalArgumentException if the given task name doesn't follow the task naming policy.
     */
    public @NonNull Builder taskName(@NonNull String taskName) {
      this.serviceId.taskName(taskName);
      return this;
    }

    /**
     * Sets the name of the environment to use for services created based on the service configuration. The environment
     * will be resolved when creating the service and decides for example which application file gets used and which
     * configuration files are updated for the environment.
     *
     * @param environment the name of the environment to use for services created based on the service configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given environment name is null.
     */
    public @NonNull Builder environment(@NonNull String environment) {
      this.serviceId.environment(environment);
      this.processConfig.environment(environment);
      return this;
    }

    /**
     * Sets the environment to use for services created based on the service configuration. The environment decides for
     * example which application file gets used and which configuration files are updated for the environment.
     *
     * @param environment the environment to use for services created based on the service configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given environment is null.
     */
    public @NonNull Builder environment(@NonNull ServiceEnvironmentType environment) {
      this.serviceId.environment(environment);
      this.processConfig.environment(environment);
      return this;
    }

    /**
     * Sets the base task id for services created based on the service configuration. Services which have the same task
     * name will <strong>never</strong> have the same task id twice. If the given task id is already taken it will get
     * counted up until either it finds a free task id or the integer limit is reached.
     * <p>
     * Note: the given task id must be either
     * <ol>
     *   <li>-1 for automatic detection of the lowest free task id.
     *   <li>positive (excluding 0) to start counting at the given task id.
     * </ol>
     *
     * @param taskId the base task id to count up from when creating services based on the configuration.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder taskId(int taskId) {
      this.serviceId.taskServiceId(taskId);
      return this;
    }

    /**
     * Sets the unique id of any service created based on the service configuration. A random uuid will be chosen if the
     * set unique id is already taken by any other service.
     *
     * @param uniqueId the base unique id for services created based on the service configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given unique id is null.
     */
    public @NonNull Builder uniqueId(@NonNull UUID uniqueId) {
      this.serviceId.uniqueId(uniqueId);
      return this;
    }

    /**
     * Sets the java command to use when starting a service based on the service configuration. If no java command is
     * set in the configuration, the configured command from the node which picked up the service will be used.
     *
     * @param javaCommand the java command to use when starting service based on the service configuration.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder javaCommand(@Nullable String javaCommand) {
      this.javaCommand = javaCommand;
      return this;
    }

    /**
     * Sets the name splitter for services based on the service configuration. The name splitter will be set between the
     * task name and the service name. For example if the name splitter is set to #, the task name to Lobby and the task
     * id is 1, the full service name will be Lobby#1.
     *
     * @param nameSplitter the name splitter to use for services created based on the service configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException     if the given name splitter is null.
     * @throws IllegalArgumentException if the given name splitter does not follow the naming pattern.
     */
    public @NonNull Builder nameSplitter(@NonNull String nameSplitter) {
      this.serviceId.nameSplitter(nameSplitter);
      return this;
    }

    /**
     * Sets the node on which services based on the service configuration should get created and started. If the given
     * node is either unknown or unable to start services, service creation requests will not work and there will be no
     * tries to move the service onto another node.
     * <p>
     * Note: this configuration option is most likely needed when trying to start a previously stored static service as
     * they are not synced in the cluster automatically.
     *
     * @param nodeUniqueId the unique id of the node which should pick up the services.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder node(@Nullable String nodeUniqueId) {
      this.serviceId.nodeUniqueId(nodeUniqueId);
      return this;
    }

    /**
     * Sets the names of the nodes which are allowed to pick up services created based on the service configuration. If
     * an empty collection is given all nodes are allowed to start the service. If specific nodes are selected the one
     * with the lowest resource usage (in percent) will be chosen to start the service. This setting has no effect if
     * one specific node was selected to start the services.
     *
     * @param allowedNodes the nodes which are allowed to start the services, an empty collection for all nodes.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given node name collection is null.
     */
    public @NonNull Builder allowedNodes(@NonNull Collection<String> allowedNodes) {
      this.serviceId.allowedNodes(allowedNodes);
      return this;
    }

    /**
     * Sets the runtime to use for the service. The runtime decides which factory is used for the service to be started
     * and in which way. An example for an external runtime (other than the default, build-in jvm runtime) is the
     * docker-jvm runtime which starts services in a docker container.
     * <p>
     * Note: if no runtime with the given name exists on the node which is picking up the service it will result in an
     * error.
     *
     * @param runtime the runtime to use for services based on the service configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given runtime is null.
     */
    public @NonNull Builder runtime(@NonNull String runtime) {
      this.runtime = runtime;
      return this;
    }

    /**
     * Sets the host address which all services based on this configuration are bound to. The host address is required
     * to be assignable on every node a service can be started on. In order to ensure that the host address is
     * assignable on every node, ip aliases can be used. Ip aliases can be defined in the config of each node. To use
     * them set the host address to the name of the alias. If null is supplied the fallback address of the node is
     * used.
     * <p>
     * Note: if the host address is not assignable or the alias is not resolvable on the node which is picking up the
     * service it will result in an error.
     *
     * @param hostAddress the host address all services based on this configuration should get bound to.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder hostAddress(@Nullable String hostAddress) {
      this.hostAddress = hostAddress;
      return this;
    }

    /**
     * Sets whether services created based on the service configuration should get deleted after being stopped. This
     * does only mean that the service gets unregistered and is no longer available for starting, but does not mean that
     * all service files get deleted when the service is static.
     * <p>
     * If this option is false the service will be stopped and then go back to the prepared state, ready to get started
     * again.
     *
     * @param autoDeleteOnStop if services should get deleted (unregistered) when stopping them.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder autoDeleteOnStop(boolean autoDeleteOnStop) {
      this.autoDeleteOnStop = autoDeleteOnStop;
      return this;
    }

    /**
     * Sets whether services created based on the service configuration should be static or not. Static services will
     * never be deleted, meaning that when stopping the service the same state can be launched again without any need to
     * deploy the current service state to a template.
     * <p>
     * Note: static services are not automatically synced between nodes, you should take care of starting the service
     * either always on the same node or ensure that the service state gets synced when it's being stopped.
     *
     * @param staticService if services created based on the configuration should be static or dynamic.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder staticService(boolean staticService) {
      this.staticService = staticService;
      return this;
    }

    /**
     * Sets the names of the groups which should get included onto any service created based on the service
     * configuration. All groups targeting the environment of the builder will automatically get included onto all
     * services without the need of explicitly defining them. If a group gets specified which is not known to the node
     * picking up the service it will silently be ignored.
     * <p>
     * This method overrides all previously added groups. The given collection will get copied into this builder,
     * meaning that changes made to the collection after the method call will not reflect into the builder and
     * vice-versa.
     *
     * @param groups the names of the groups to include on all services.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given group name collection is null.
     */
    public @NonNull Builder groups(@NonNull Collection<String> groups) {
      this.groups = new HashSet<>(groups);
      return this;
    }

    /**
     * Modifies the names of the groups which should get included onto any service created based on the service
     * configuration. All groups targeting the environment of the builder will automatically get included onto all
     * services without the need of explicitly defining them. If a group gets specified which is not known to the node
     * picking up the service it will silently be ignored.
     *
     * @param modifier the modifier to be applied to the already added groups of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given group name collection is null.
     */
    public @NonNull Builder modifyGroups(@NonNull Consumer<Collection<String>> modifier) {
      modifier.accept(this.groups);
      return this;
    }

    /**
     * Sets the files which should get deleted when stopping a service created based on the configuration. Any path in
     * the given collection can either represent a single file or directory, but must be inside the service directory.
     * Path traversal to leave the service directory will result in an exception.
     * <p>
     * This method will override all previously added file deletions. The given collection will be copied into this
     * builder, meaning that changes made to the collection after the method call will not reflect into the builder and
     * vice-versa.
     *
     * @param deletedFilesAfterStop the files to delete when a service based on the configuration gets stopped.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given file name collection is null.
     */
    public @NonNull Builder deletedFilesAfterStop(@NonNull Collection<String> deletedFilesAfterStop) {
      this.deletedFilesAfterStop = new HashSet<>(deletedFilesAfterStop);
      return this;
    }

    /**
     * Modifies the files which should get deleted when stopping a service created based on the configuration. Any path
     * in the given collection can either represent a single file or directory, but must be inside the service
     * directory. Path traversal to leave the service directory will result in an exception.
     *
     * @param modifier the modifier to be applied to the already added files of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given file name collection is null.
     */
    public @NonNull Builder modifyDeletedFilesAfterStop(@NonNull Consumer<Collection<String>> modifier) {
      modifier.accept(this.deletedFilesAfterStop);
      return this;
    }

    /**
     * Sets the maximum heap memory (in MB) a service based on this configuration is allowed to allocate. The given heap
     * memory size must be at least 50 MB (less heap memory makes no sense when running a service).
     *
     * @param maxHeapMemory the maximum heap memory a service is allowed to use.
     * @return the same instance as used to call the method, for chaining.
     * @throws IllegalArgumentException if the given memory size is less than 50 mb.
     */
    public @NonNull Builder maxHeapMemory(int maxHeapMemory) {
      this.processConfig.maxHeapMemorySize(maxHeapMemory);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder jvmOptions(@NonNull Collection<String> jvmOptions) {
      this.processConfig.jvmOptions(jvmOptions);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder modifyJvmOptions(@NonNull Consumer<Collection<String>> jvmOptions) {
      this.processConfig.modifyJvmOptions(jvmOptions);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder processParameters(@NonNull Collection<String> processParameters) {
      this.processConfig.processParameters(processParameters);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder modifyProcessParameters(@NonNull Consumer<Collection<String>> modifier) {
      this.processConfig.modifyProcessParameters(modifier);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder environmentVariables(@NonNull Map<String, String> environmentVariables) {
      this.processConfig.environmentVariables(environmentVariables);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder modifyEnvironmentVariables(@NonNull Consumer<Map<String, String>> modifier) {
      this.processConfig.modifyEnvironmentVariables(modifier);
      return this;
    }

    /**
     * Sets the start port for services created based on the configuration. If the given port is already taken by any
     * other process it gets counted up until it either reaches the port limit (65535) or finds a free port to start the
     * service on. If no free port was found an exception is thrown and the service will not start.
     *
     * @param startPort the port to start services upwards from.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder startPort(@Range(from = 0, to = 0xFFFF) int startPort) {
      this.port = startPort;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NonNull Builder self() {
      return this;
    }

    /**
     * Builds a service configuration based on all previously supplied properties. <strong>NOTE:</strong> further
     * changes to this builder might reflect into the service configuration build from it. Do not re-use a builder,
     * always use a new one.
     *
     * @return a new service configuration based on this builder.
     * @throws NullPointerException     if one of the required properties is either not set or invalid.
     * @throws IllegalArgumentException if the given port is out of range.
     */
    @Override
    public @NonNull ServiceConfiguration build() {
      Preconditions.checkArgument(this.port > 0 && this.port <= 0xFFFF, "invalid port provided");
      return new ServiceConfiguration(
        this.serviceId.build(),
        this.processConfig.build(),
        this.retryConfiguration,
        this.port,
        this.runtime,
        this.hostAddress,
        this.javaCommand,
        this.autoDeleteOnStop,
        this.staticService,
        Set.copyOf(this.groups),
        Set.copyOf(this.deletedFilesAfterStop),
        Set.copyOf(this.templates),
        Set.copyOf(this.deployments),
        Set.copyOf(this.includes),
        this.properties.immutableCopy());
    }
  }
}
