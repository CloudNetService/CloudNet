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
import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.document.Document;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The main base configuration for all services within the CloudNet cluster. A task is normally permanent stored
 * somewhere to allow loads after a node restart. Therefore, service tasks are the main configuration point for everyone
 * who needs either permanent storing of a service configuration or who doesn't work with the CloudNet api to start a
 * service.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode(callSuper = true)
public class ServiceTask extends ServiceConfigurationBase implements Cloneable, Named {

  /**
   * The jvm static pattern which validate that a task or service name is acceptable. It for example doesn't allow
   * slashes to prevent navigation out of the service directory itself (path traversal).
   */
  public static final String NAMING_REGEX = "^[a-zA-Z\\d._\\-*]+$";
  public static final Pattern NAMING_PATTERN = Pattern.compile(NAMING_REGEX);

  private final String name;
  private final String runtime;
  private final String hostAddress;
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

  /**
   * Constructs a new service task instance.
   *
   * @param name                  the name of the service task.
   * @param runtime               the runtime, used to determine the service factory for services of the task.
   * @param hostAddress           the host address all services of this task are bound to.
   * @param javaCommand           the overridden java command to use, if null the node specific one will be used.
   * @param nameSplitter          the splitter to put into the service name, between the task name and service id.
   * @param disableIpRewrite      true if the ip set in a service configuration file should not get touched.
   * @param maintenance           true if the task should be in maintenance.
   * @param autoDeleteOnStop      true if services based on the task should get deleted when stopping them.
   * @param staticServices        true if services created based on the task should be static.
   * @param groups                the groups to automatically include to all services started based on the task.
   * @param associatedNodes       the nodes which are allowed to pickup services based on the task.
   * @param deletedFilesAfterStop the files which should automatically get deleted when stopping a service.
   * @param processConfiguration  the process configuration of the task.
   * @param startPort             the start port of the task.
   * @param minServiceCount       the amount of services which should be online by default.
   * @param templates             the templates to include on each service created based on the task.
   * @param deployments           the deployments which should be added initially to each service.
   * @param includes              the includes which should be added initially to each service.
   * @param properties            the properties of the task.
   * @throws NullPointerException if one of the parameters (except the java command) is null.
   */
  protected ServiceTask(
    @NonNull String name,
    @NonNull String runtime,
    @Nullable String hostAddress,
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
    @NonNull Document properties
  ) {
    super(templates, deployments, includes, properties);
    this.name = name;
    this.runtime = runtime;
    this.hostAddress = hostAddress;
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

  /**
   * Constructs a new builder for a service task.
   *
   * @return a new service task builder.
   */
  public static @NonNull ServiceTask.Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder for a service task which has the same properties set than the given service task.
   * <p>
   * When calling build directly after constructing a builder using this method, it will result in a service task which
   * is equal but not the same as the given one.
   *
   * @param serviceTask the service task to copy the properties of.
   * @return a builder for a service task with the properties of the given one already set.
   * @throws NullPointerException if the given service task is null.
   */
  public static @NonNull ServiceTask.Builder builder(@NonNull ServiceTask serviceTask) {
    return builder()
      .name(serviceTask.name())
      .javaCommand(serviceTask.javaCommand())
      .runtime(serviceTask.runtime())
      .hostAddress(serviceTask.hostAddress())
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
      .environmentVariables(serviceTask.environmentVariables())

      .templates(serviceTask.templates())
      .deployments(serviceTask.deployments())
      .inclusions(serviceTask.inclusions())

      .startPort(serviceTask.startPort())
      .minServiceCount(serviceTask.minServiceCount())

      .properties(serviceTask.propertyHolder())
      .processConfiguration(ProcessConfiguration.builder(serviceTask.processConfiguration()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.name;
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
   * Get the host address that all services of this task are bound to. The host address is not required to be an ip
   * address, it is possible that the host address is just an ip alias which needs to be resolved using the
   * configuration of the node. The host address might be null, in that case the fallback host of the node is used.
   *
   * @return the host address all services of this task are bound to.
   */
  public @Nullable String hostAddress() {
    return this.hostAddress;
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
   * Get the splitter to put between the name of the task and the numeric id of it when creating a full display name
   * variant of the underlying service.
   *
   * @return the name splitter of the underlying service.
   */
  public @NonNull String nameSplitter() {
    return this.nameSplitter;
  }

  /**
   * Get if rewriting the ip set in the service configuration file (for example the server.properties file) should be
   * disabled and the set ip should be used. Note that if disabled the node starting the service will not read the ip
   * from the configuration, therefore the configured ip of the node is still used as the service address.
   *
   * @return true if the node starting a service based on the task should not change the ip in the service config.
   */
  public boolean disableIpRewrite() {
    return this.disableIpRewrite;
  }

  /**
   * Get if this service task is in maintenance. This option is used to restrict for example who is able to join on a
   * service. On the other hand, CloudNet will not try to start any service of tasks which are in maintenance even if
   * the configured minimum service count is more than 0.
   *
   * @return true if this task is in maintenance, false otherwise.
   */
  public boolean maintenance() {
    return this.maintenance;
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
  public boolean staticServices() {
    return this.staticServices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<String> jvmOptions() {
    return this.processConfiguration.jvmOptions();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<String> processParameters() {
    return this.processConfiguration.processParameters();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Unmodifiable
  public @NonNull Map<String, String> environmentVariables() {
    return this.processConfiguration.environmentVariables();
  }

  /**
   * Get the names of the groups whose configuration should get included when starting a service based on this
   * configuration. Each configuration is only included when it is present on the node starting the service, if not it
   * will silently be ignored. Note that groups targeting the same environment as this configuration will get included
   * automatically.
   *
   * @return the names of the group configurations to include their configurations before starting.
   */
  public @NonNull Collection<String> groups() {
    return this.groups;
  }

  /**
   * Get the unique ids of all nodes which are allowed to pick up and manage the underlying service. This method returns
   * an empty collection if all nodes are allowed to pick up the service, but never null.
   * <p>
   * Note: this method might return an empty collection if a specific node was already chosen, you might need to check
   * that too.
   *
   * @return the unique ids of the nodes which are allowed to pick up and manage the underlying service.
   */
  public @NonNull Collection<String> associatedNodes() {
    return this.associatedNodes;
  }

  /**
   * Get the files (or directories) which should get deleted when stopping the service (before deployments are
   * executed). Trying to delete files outside the service directory will result in an exception.
   *
   * @return a set of path to files/directories which should get deleted when stopping a service based on this config.
   */
  public @NonNull Collection<String> deletedFilesAfterStop() {
    return this.deletedFilesAfterStop;
  }

  /**
   * Get the process configuration to apply to all services which get created based on this configuration.
   *
   * @return the process configuration to apply to all services.
   */
  public @NonNull ProcessConfiguration processConfiguration() {
    return this.processConfiguration;
  }

  /**
   * The port of the service to start on. If the given port is already taken it will be counted up until it reaches the
   * port limit (65535) or one of the ports in between is free to be taken. Port changes because of counting up will not
   * be reflected into this configuration and vice-versa.
   *
   * @return the port number to start the service on, might be counted up when the port is already taken.
   */
  public @Range(from = 1, to = 0xFFFF) int startPort() {
    return this.startPort;
  }

  /**
   * Get the amount of services which should be always online if this task is not in maintenance. CloudNet tries to
   * start services until the minimum amount of them is online if possible, if for example no nodes are online which are
   * able to pick up the service, the minimum service count might not get reached.
   *
   * @return the minimum amount of services which CloudNet should try to keep online.
   */
  public @Range(from = 0, to = Integer.MAX_VALUE) int minServiceCount() {
    return this.minServiceCount;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceTask clone() {
    try {
      return (ServiceTask) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  /**
   * A builder for a service task.
   *
   * @since 4.0
   */
  public static class Builder extends ServiceConfigurationBase.Builder<ServiceTask, Builder> {

    private String name;
    private String hostAddress;
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

    /**
     * Sets the name of the service task to use. This name must conform the naming pattern defined in the service task
     * class.
     *
     * @param name the name of the service task to use.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given name is null.
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
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
     * Sets the host address which all services of this task are bound to. The host address is required to be assignable
     * on every node a service can be started on. In order to ensure that the host address is assignable on every node,
     * ip aliases can be used. Ip aliases can be defined in the config of each node. To use them set the host address to
     * the name of the alias. If null is supplied the fallback address of the node is used.
     * <p>
     * Note: if the host address is not assignable or the alias is not resolvable on the node which is picking up the
     * service it will result in an error.
     *
     * @param hostAddress the host address to bind services of this task to.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder hostAddress(@Nullable String hostAddress) {
      this.hostAddress = hostAddress;
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
     * @throws NullPointerException if the given name splitter is null.
     */
    public @NonNull Builder nameSplitter(@NonNull String nameSplitter) {
      this.nameSplitter = nameSplitter;
      return this;
    }

    /**
     * Sets if rewriting the ip set in the service configuration file (for example the server.properties file) should be
     * disabled and the set ip should be used. Note that if disabled the node starting the service will not read the ip
     * from the configuration, therefore the configured ip of the node is still used as the service address.
     *
     * @param disableIpRewrite if ip rewriting should be disabled.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder disableIpRewrite(boolean disableIpRewrite) {
      this.disableIpRewrite = disableIpRewrite;
      return this;
    }

    /**
     * Sets if this service task is in maintenance. This option is used to restrict for example who is able to join on a
     * service. On the other hand, CloudNet will not try to start any service of tasks which are in maintenance even if
     * the configured minimum service count is more than 0.
     *
     * @param maintenance if the task should be in maintenance.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder maintenance(boolean maintenance) {
      this.maintenance = maintenance;
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
     * @param staticServices if services created based on the configuration should be static or dynamic.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder staticServices(boolean staticServices) {
      this.staticServices = staticServices;
      return this;
    }

    /**
     * Sets the names of the nodes which are allowed to pick up services created based on the service configuration. If
     * an empty collection is given all nodes are allowed to start the service. If specific nodes are selected the one
     * with the lowest resource usage (in percent) will be chosen to start the service. This setting has no effect if
     * one specific node was selected to start the services.
     *
     * @param associatedNodes the nodes which are allowed to start the services, an empty collection for all nodes.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given node name collection is null.
     */
    public @NonNull Builder associatedNodes(@NonNull Collection<String> associatedNodes) {
      this.associatedNodes = new HashSet<>(associatedNodes);
      return this;
    }

    /**
     * Modifies nodes which are allowed to pick up services created based on the service configuration. If specific
     * nodes are selected the one with the lowest resource usage (in percent) will be chosen to start the service. This
     * setting has no effect if one specific node was selected to start the services.
     *
     * @param modifier the modifier to be applied to the already added allowed nodes of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given node name is null.
     */
    public @NonNull Builder modifyAssociatedNodes(@NonNull Consumer<Collection<String>> modifier) {
      modifier.accept(this.associatedNodes);
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
     * Modifies groups which should get included onto any service created based on the service configuration. All groups
     * targeting the environment of the builder will automatically get included onto all services without the need of
     * explicitly defining them. If a group gets specified which is not known to the node picking up the service it will
     * silently be ignored.
     *
     * @param modifier the modifier to be applied to the already added groups of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given group name is null.
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
    public @NonNull Builder modifyDeletedFileAfterStop(@NonNull Consumer<Collection<String>> modifier) {
      modifier.accept(this.deletedFilesAfterStop);
      return this;
    }

    /**
     * Sets the process configuration builder of this builder. Further calls might overwrite changes in the given
     * builder, for example when setting the max heap memory services are allowed to use.
     *
     * @param processConfiguration the new process configuration to use.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given process configuration builder is null.
     */
    public @NonNull Builder processConfiguration(@NonNull ProcessConfiguration.Builder processConfiguration) {
      this.processConfiguration = processConfiguration;
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
    public @NonNull Builder startPort(int startPort) {
      this.startPort = startPort;
      return this;
    }

    /**
     * Sets the amount of services which should be always online if this task is not in maintenance. CloudNet tries to
     * start services until the minimum amount of them is online if possible, if for example no nodes are online which
     * are able to pick up the service, the minimum service count might not get reached.
     *
     * @param minServiceCount the minimum amount of services CloudNet should try to keep online.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder minServiceCount(int minServiceCount) {
      this.minServiceCount = minServiceCount;
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
      this.processConfiguration.maxHeapMemorySize(maxHeapMemory);
      return this;
    }

    /**
     * Sets the environment to use for services created based on the service configuration. The environment decides for
     * example which application file gets used and which configuration files are updated for the environment. If no
     * default start port is yet set the default port of the service environment will be used.
     *
     * @param serviceEnvironmentType the environment to use for services created based on the service configuration.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given environment is null.
     */
    public @NonNull Builder serviceEnvironmentType(@NonNull ServiceEnvironmentType serviceEnvironmentType) {
      this.processConfiguration.environment(serviceEnvironmentType);
      this.startPort = this.startPort == -1 ? serviceEnvironmentType.defaultStartPort() : this.startPort;

      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder jvmOptions(@NonNull Collection<String> jvmOptions) {
      this.processConfiguration.jvmOptions(jvmOptions);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder modifyJvmOptions(@NonNull Consumer<Collection<String>> modifier) {
      this.processConfiguration.modifyJvmOptions(modifier);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder processParameters(@NonNull Collection<String> processParameters) {
      this.processConfiguration.processParameters(processParameters);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder modifyProcessParameters(@NonNull Consumer<Collection<String>> modifier) {
      this.processConfiguration.modifyProcessParameters(modifier);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder environmentVariables(@NonNull Map<String, String> environmentVariables) {
      this.processConfiguration.environmentVariables(environmentVariables);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Builder modifyEnvironmentVariables(@NonNull Consumer<Map<String, String>> modifier) {
      this.processConfiguration.modifyEnvironmentVariables(modifier);
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
     * {@inheritDoc}
     *
     * @throws NullPointerException     if no task name is given.
     * @throws IllegalArgumentException if the start port is invalid.
     */
    @Override
    public @NonNull ServiceTask build() {
      Preconditions.checkNotNull(this.name, "no name given");
      Preconditions.checkArgument(this.startPort > 0 && this.startPort <= 0xFFFF, "Invalid start port given");

      return new ServiceTask(
        this.name,
        this.runtime,
        this.hostAddress,
        this.javaCommand,
        this.nameSplitter,
        this.disableIpRewrite,
        this.maintenance,
        this.autoDeleteOnStop,
        this.staticServices,
        Set.copyOf(this.groups),
        Set.copyOf(this.associatedNodes),
        Set.copyOf(this.deletedFilesAfterStop),
        this.processConfiguration.build(),
        this.startPort,
        this.minServiceCount,
        Set.copyOf(this.templates),
        Set.copyOf(this.deployments),
        Set.copyOf(this.includes),
        this.properties.immutableCopy());
    }
  }
}
