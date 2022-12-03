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

package eu.cloudnetservice.driver;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.permission.PermissionServiceSetEvent;
import eu.cloudnetservice.driver.module.DefaultModuleProvider;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.driver.network.rpc.defaults.DefaultRPCFactory;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.DefaultRPCHandlerRegistry;
import eu.cloudnetservice.driver.network.rpc.defaults.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.provider.CloudMessenger;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.DefaultServiceRegistry;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The main entrypoint to work with the CloudNet api. Each component which is supported and represents a driver instance
 * will extend this class and provide a singleton instance to use for developers.
 *
 * @since 4.0
 */
@Deprecated(forRemoval = true)
public abstract class CloudNetDriver {

  @VisibleForTesting
  static CloudNetDriver instance;

  static {
    // the default locale used when formatting a string (f. ex. via String.format).
    // this ensures that the result is always consistent when formatting anywhere.
    // we could as well set the global default locale, but that would affect every locale
    // sensitive operation which we be unsafe (f. ex. calling String#toLowerCase).
    Locale.setDefault(Locale.Category.FORMAT, Locale.ROOT);
  }

  protected final CloudNetVersion cloudNetVersion;
  protected final List<String> commandLineArguments;
  protected final DriverEnvironment driverEnvironment;

  protected final EventManager eventManager = null;// new DefaultEventManager();
  protected final ModuleProvider moduleProvider = new DefaultModuleProvider();
  protected final ServiceRegistry serviceRegistry = new DefaultServiceRegistry();
  protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

  protected final RPCHandlerRegistry rpcHandlerRegistry = new DefaultRPCHandlerRegistry();
  protected final RPCFactory rpcFactory = new DefaultRPCFactory(
    DefaultObjectMapper.DEFAULT_MAPPER,
    DataBufFactory.defaultFactory());

  protected PermissionManagement permissionManagement;

  protected NetworkClient networkClient;

  protected CloudMessenger messenger;
  protected CloudServiceFactory cloudServiceFactory;

  protected DatabaseProvider databaseProvider;
  protected ClusterNodeProvider clusterNodeProvider;
  protected ServiceTaskProvider serviceTaskProvider;
  protected CloudServiceProvider cloudServiceProvider;
  protected TemplateStorageProvider templateStorageProvider;
  protected GroupConfigurationProvider groupConfigurationProvider;

  /**
   * Constructs a new CloudNet driver instance. This constructor shouldn't be used directly and is only accessible for
   * any class which provides a custom implementation of CloudNet. Use {@link #instance()} to obtain the current driver
   * instance of the environment instead.
   *
   * @param version     the version of the cloud which is currently running.
   * @param args        the command line arguments which were supplied to the launcher when starting.
   * @param environment the environment in which the driver is currently running.
   * @throws NullPointerException if the given version, command line args or environment is null.
   */
  protected CloudNetDriver(
    @NonNull CloudNetVersion version,
    @NonNull List<String> args,
    @NonNull DriverEnvironment environment
  ) {
    this.cloudNetVersion = version;
    this.commandLineArguments = args;
    this.driverEnvironment = environment;
  }

  /**
   * Get the singleton instance of the current environment of the CloudNet driver. This instance is initialized once
   * when starting the current environment and will never change during the lifetime of the jvm. Therefore, the instance
   * of the environment can be safely cached.
   * <p>
   * This method uses a generic to support easier accessibility. So instead of writing
   * <pre>
   * {@code
   *  public static final Wrapper WRAPPER = Wrapper.instance();
   * }
   * </pre>
   * You can also use
   * <pre>
   * {@code
   *  public static final Wrapper WRAPPER = CloudNetDriver.instance();
   * }
   * </pre>
   * Keep in mind that requesting a wrong type of instance (for example a wrapper instance in the node environment) this
   * method will result in an exception.
   *
   * @return the current, jvm static instance of the current environment.
   */
  @SuppressWarnings("unchecked")
  @Deprecated(forRemoval = true)
  public static @NonNull <T extends CloudNetDriver> T instance() {
    return (T) CloudNetDriver.instance;
  }

  /**
   * Sets the current instance of the environment. The instance of the driver is jvm static, this method call will
   * result in an exception if the instance was already set. This prevents accidental overrides of the instance and
   * unintended behaviours of plugins and nodes.
   * <p>
   * By default, there is no need to use this method ever.
   *
   * @param instance the instance of the CloudNet driver to use.
   * @throws NullPointerException  if the given instance is null.
   * @throws IllegalStateException if the instance of the driver was already set previously.
   */
  protected static void instance(@NonNull CloudNetDriver instance) {
    Preconditions.checkState(CloudNetDriver.instance == null, "Singleton instance can only be set once");
    CloudNetDriver.instance = instance;
  }

  /**
   * Starts the current environment. This method is called internally after all the boot preparation was done and should
   * not be called from the api at any point. Duplicate executions of this method will not result in an exceptions, but
   * might lead to unexpected results in api or modules which depend on the underlying implementations.
   *
   * @param startInstant the instant of the actual jvm entry point invocation.
   * @throws Exception            if any exception occurs while starting the environment.
   * @throws NullPointerException if the given start instant is null.
   */
  protected abstract void start(@NonNull Instant startInstant) throws Exception;

  /**
   * Stops the current environment and frees all resources which are associated with it. Further, calls to any api in
   * this class might result in exceptions or unexpected results. Duplicate invocation of this method should not execute
   * the shutdown lifecycle twice.
   */
  public abstract void stop();

  /**
   * Returns the name of the component associated with the current environment. This will (by default) either be the
   * current id of the node, or the name of the service. This information is static during the component lifetime and
   * can be cached safely.
   *
   * @return the name of the component.
   */
  public abstract @NonNull String componentName();

  /**
   * Gets the unique id of the node which is associated with the current component. This will (by default) return the
   * unique id of the current node if called on a node and the unique id of the node the process is running on if called
   * from a service. This information is static during the component lifetime and can be cached safely.
   *
   * @return the uniqueId of the node which is associated with the current component.
   */
  public abstract @NonNull String nodeUniqueId();

  /**
   * Get the current cloud service factory to create new services. This information is static during the component
   * lifetime and can be cached safely.
   *
   * @return the current cloud service factory.
   */
  public @NonNull CloudServiceFactory cloudServiceFactory() {
    return this.cloudServiceFactory;
  }

  /**
   * Get the current provider for service tasks. This information is static during the component lifetime and can be
   * cached safely.
   *
   * @return the current service task provider.
   */
  public @NonNull ServiceTaskProvider serviceTaskProvider() {
    return this.serviceTaskProvider;
  }

  /**
   * Get the current cluster node server provider. This information is static during the component lifetime and can be
   * cached safely.
   *
   * @return the current cluster node server provider.
   */
  public @NonNull ClusterNodeProvider clusterNodeProvider() {
    return this.clusterNodeProvider;
  }

  /**
   * Get the current group configuration provider. This information is static during the component lifetime and can be
   * cached safely.
   *
   * @return the current group configuration provider.
   */
  public @NonNull GroupConfigurationProvider groupConfigurationProvider() {
    return this.groupConfigurationProvider;
  }

  /**
   * Get the current template storage provider. This information is static during the component lifetime and can be
   * cached safely.
   *
   * @return the current template storage provider.
   */
  public @NonNull TemplateStorageProvider templateStorageProvider() {
    return this.templateStorageProvider;
  }

  /**
   * Get the current cloud messenger. This information is static during the component lifetime and can be cached
   * safely.
   *
   * @return the current cloud messenger.
   */
  public @NonNull CloudMessenger messenger() {
    return this.messenger;
  }

  /**
   * Get the current database provider.  This information is static during the component lifetime and can be cached
   * safely.
   *
   * @return the current database provider.
   */
  public @NonNull DatabaseProvider databaseProvider() {
    return this.databaseProvider;
  }

  /**
   * Get the current cloud service provider. This information is static during the component lifetime and can be cached
   * safely.
   *
   * @return the current cloud service provider.
   */
  public @NonNull CloudServiceProvider cloudServiceProvider() {
    return this.cloudServiceProvider;
  }

  /**
   * Get the current network client. This information is static during the component lifetime and can be cached safely.
   *
   * @return the current network client.
   */
  public @NonNull NetworkClient networkClient() {
    return this.networkClient;
  }

  /**
   * Get the current service registry. This information is static during the component lifetime and can be cached
   * safely.
   *
   * @return the current service registry.
   */
  public @NonNull ServiceRegistry serviceRegistry() {
    return this.serviceRegistry;
  }

  /**
   * Get the current event manager instance. This information is static during the component lifetime and can be cached
   * safely.
   *
   * @return the current event manager.
   */
  public @NonNull EventManager eventManager() {
    return this.eventManager;
  }

  /**
   * Get the current module provider instance. This information is static during the component lifetime and can be
   * cached safely.
   *
   * @return the current module provider.
   */
  public @NonNull ModuleProvider moduleProvider() {
    return this.moduleProvider;
  }

  /**
   * Get the current task executor instance. This information is static during the component lifetime and can be cached
   * safely. When this component stops all tasks in that scheduler will be terminated as well.
   *
   * @return the current task executor.
   */
  public @NonNull ScheduledExecutorService taskExecutor() {
    return this.scheduler;
  }

  /**
   * Get the current rpc factory instance. This information is static during the component lifetime and can be cached
   * safely.
   *
   * @return the current rpc factory.
   */
  public @NonNull RPCFactory rpcFactory() {
    return this.rpcFactory;
  }

  /**
   * Get the current registry for rpc handlers. This information is static during the component lifetime and can be
   * cached safely.
   *
   * @return the current rpc handler registry.
   */
  public @NonNull RPCHandlerRegistry rpcHandlerRegistry() {
    return this.rpcHandlerRegistry;
  }

  /**
   * Get the current environment in which this driver implementation runs. This information is static during the
   * component lifetime and can be cached safely.
   *
   * @return the current driver environment.
   */
  public @NonNull DriverEnvironment environment() {
    return this.driverEnvironment;
  }

  /**
   * Get the current version of this driver implementation. This information is static during the component lifetime and
   * can be cached safely.
   *
   * @return the current version of this driver.
   */
  public @NonNull CloudNetVersion version() {
    return this.cloudNetVersion;
  }

  /**
   * Get the supplied command line arguments to the initial jvm entry point. This information is static during the
   * component lifetime and can be cached safely.
   *
   * @return the supplied command line options.
   */
  @UnmodifiableView
  public @NonNull List<String> commandLineArguments() {
    return Collections.unmodifiableList(this.commandLineArguments);
  }

  /**
   * Get the current permission management of this driver instance. This information is not static during the component
   * lifetime and might change. This method throws an exception when no permission management is set instead of
   * returning null.
   *
   * @return the current permission management of this driver instance.
   * @throws NullPointerException if no permission management was set in this instance.
   */
  public @NonNull PermissionManagement permissionManagement() {
    Preconditions.checkNotNull(this.permissionManagement, "no permission management available");
    return this.permissionManagement;
  }

  /**
   * Sets the given permission management as the current permission management. This method throws an exception if the
   * new permission management cannot override the current permission management because it is either
   * <ol>
   *   <li>no overridable.
   *   <li>not assignable to the new permission management.
   * </ol>
   *
   * @param management the new permission management to use.
   * @throws NullPointerException     if the given permission management is null.
   * @throws IllegalArgumentException if the given permission management cannot override the current one.
   */
  public void permissionManagement(@NonNull PermissionManagement management) {
    // if there is no old permission management or the old permission management can be overridden
    // we can just set the new one
    if (this.permissionManagement == null || this.permissionManagement.allowsOverride()) {
      // close the old permission management
      if (this.permissionManagement != null) {
        this.permissionManagement.close();
      }
      // update the current permission management
      this.permissionManagement = management;
      this.eventManager.callEvent(new PermissionServiceSetEvent(management));
      return;
    }
    // check if the new permission management is assignable to the old permission management
    if (this.permissionManagement.getClass().isAssignableFrom(management.getClass())) {
      // close the old permission management
      this.permissionManagement.close();
      // update the current permission management
      this.permissionManagement = management;
      this.eventManager.callEvent(new PermissionServiceSetEvent(management));
      return;
    }
    // the permission management cannot be set
    throw new IllegalArgumentException(String.format(
      "Permission management %s does not meet the requirements to override the current permission management %s",
      management,
      this.permissionManagement));
  }
}
