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

package eu.cloudnetservice.cloudnet.driver;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.driver.database.DatabaseProvider;
import eu.cloudnetservice.cloudnet.driver.event.DefaultEventManager;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.event.events.permission.PermissionServiceSetEvent;
import eu.cloudnetservice.cloudnet.driver.module.DefaultModuleProvider;
import eu.cloudnetservice.cloudnet.driver.module.ModuleProvider;
import eu.cloudnetservice.cloudnet.driver.network.NetworkClient;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCHandlerRegistry;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.DefaultRPCFactory;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.handler.DefaultRPCHandlerRegistry;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionManagement;
import eu.cloudnetservice.cloudnet.driver.provider.CloudMessenger;
import eu.cloudnetservice.cloudnet.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.cloudnet.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.cloudnet.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.cloudnet.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.cloudnet.driver.registry.DefaultServiceRegistry;
import eu.cloudnetservice.cloudnet.driver.registry.ServiceRegistry;
import eu.cloudnetservice.cloudnet.driver.template.TemplateStorage;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.jetbrains.annotations.VisibleForTesting;

public abstract class CloudNetDriver {

  private static CloudNetDriver instance;

  protected final List<String> commandLineArguments;

  protected final EventManager eventManager = new DefaultEventManager();
  protected final ModuleProvider moduleProvider = new DefaultModuleProvider();
  protected final ServiceRegistry serviceRegistry = new DefaultServiceRegistry();
  protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

  protected final RPCHandlerRegistry rpcHandlerRegistry = new DefaultRPCHandlerRegistry();
  protected final RPCFactory rpcFactory = new DefaultRPCFactory(
    DefaultObjectMapper.DEFAULT_MAPPER,
    DataBufFactory.defaultFactory());

  protected CloudNetVersion cloudNetVersion;
  protected PermissionManagement permissionManagement;

  protected NetworkClient networkClient;
  protected CloudServiceFactory cloudServiceFactory;

  protected CloudMessenger messenger;
  protected DatabaseProvider databaseProvider;
  protected ClusterNodeProvider clusterNodeProvider;
  protected ServiceTaskProvider serviceTaskProvider;
  protected CloudServiceProvider cloudServiceProvider;
  protected GroupConfigurationProvider groupConfigurationProvider;

  protected DriverEnvironment driverEnvironment = DriverEnvironment.EMBEDDED;

  protected CloudNetDriver(@NonNull List<String> args) {
    this.commandLineArguments = args;
  }

  /**
   * @return a singleton instance of the CloudNetDriver
   */
  public static @NonNull CloudNetDriver instance() {
    return CloudNetDriver.instance;
  }

  @VisibleForTesting
  protected static void instance(@NonNull CloudNetDriver instance) {
    CloudNetDriver.instance = instance;
  }

  public abstract void start(@NonNull Instant startInstant) throws Exception;

  public abstract void stop();

  /**
   * Returns the name of this component. (e.g. Node-1, Lobby-1)
   */
  public abstract @NonNull String componentName();

  /**
   * Gets the component name if it is called on a node and the name of the node that the service is started on if called
   * on the wrapper
   *
   * @return the uniqueId of the current node
   */
  public abstract @NonNull String nodeUniqueId();

  /**
   * @return the set {@link CloudServiceFactory} for creating new services in the cloud
   */
  public @NonNull CloudServiceFactory cloudServiceFactory() {
    return this.cloudServiceFactory;
  }

  /**
   * @return the set {@link ServiceTaskProvider} for the management of service tasks
   */
  public @NonNull ServiceTaskProvider serviceTaskProvider() {
    return this.serviceTaskProvider;
  }

  /**
   * @return the set {@link ClusterNodeProvider} which provides access to the local node or nodes in the cluster.
   */
  public @NonNull ClusterNodeProvider clusterNodeProvider() {
    return this.clusterNodeProvider;
  }

  /**
   * @return the set {@link GroupConfigurationProvider} which manages the services groups
   */
  public @NonNull GroupConfigurationProvider groupConfigurationProvider() {
    return this.groupConfigurationProvider;
  }

  /**
   * @return the set {@link CloudMessenger} to communicate between services and nodes
   */
  public @NonNull CloudMessenger messenger() {
    return this.messenger;
  }

  /**
   * @return the current {@link PermissionManagement}
   * @throws NullPointerException if there is no PermissionManagement available
   */
  public @NonNull PermissionManagement permissionManagement() {
    Preconditions.checkNotNull(this.permissionManagement, "no permission management available");
    return this.permissionManagement;
  }

  /**
   * Sets an {@link PermissionManagement} as current PermissionManagement.
   *
   * @param management the {@link PermissionManagement} to be set
   * @throws IllegalStateException if the current {@link PermissionManagement} does not allow overwriting it and the
   *                               class names are not the same
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

  /**
   * Returns the local {@link TemplateStorage} of the Node.
   *
   * @return the local {@link TemplateStorage}
   * @throws IllegalStateException if the TemplateStorage is not present
   */

  public abstract @NonNull TemplateStorage localTemplateStorage();

  /**
   * @param storage the name of the storage
   * @return the registered {@link TemplateStorage}, null if the storage does not exist
   */
  public abstract @Nullable TemplateStorage templateStorage(@NonNull String storage);

  public abstract @NonNull Collection<TemplateStorage> availableTemplateStorages();

  public @NonNull Task<Collection<TemplateStorage>> availableTemplateStoragesAsync() {
    return Task.supply(this::availableTemplateStorages);
  }

  public @NonNull DatabaseProvider databaseProvider() {
    return this.databaseProvider;
  }

  public @NonNull CloudServiceProvider cloudServiceProvider() {
    return this.cloudServiceProvider;
  }

  public @NonNull NetworkClient networkClient() {
    return this.networkClient;
  }

  public @NonNull ServiceRegistry serviceRegistry() {
    return this.serviceRegistry;
  }

  public @NonNull EventManager eventManager() {
    return this.eventManager;
  }

  public @NonNull ModuleProvider moduleProvider() {
    return this.moduleProvider;
  }

  public @NonNull ScheduledExecutorService taskExecutor() {
    return this.scheduler;
  }

  public @NonNull RPCFactory rpcProviderFactory() {
    return this.rpcFactory;
  }

  public @NonNull RPCHandlerRegistry rpcHandlerRegistry() {
    return this.rpcHandlerRegistry;
  }

  public @NonNull DriverEnvironment environment() {
    return this.driverEnvironment;
  }

  public @NonNull CloudNetVersion version() {
    return this.cloudNetVersion;
  }

  @UnmodifiableView
  public @NonNull List<String> commandLineArguments() {
    return Collections.unmodifiableList(this.commandLineArguments);
  }
}
