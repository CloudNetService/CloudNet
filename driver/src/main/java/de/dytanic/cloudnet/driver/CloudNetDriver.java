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

package de.dytanic.cloudnet.driver;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.registry.DefaultServicesRegistry;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.database.DatabaseProvider;
import de.dytanic.cloudnet.driver.event.DefaultEventManager;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionServiceSetEvent;
import de.dytanic.cloudnet.driver.module.DefaultModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandlerRegistry;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.defaults.DefaultRPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.DefaultRPCHandlerRegistry;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.provider.NodeInfoProvider;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.jetbrains.annotations.VisibleForTesting;

public abstract class CloudNetDriver {

  private static CloudNetDriver instance;

  protected final List<String> commandLineArguments;

  protected final IEventManager eventManager = new DefaultEventManager();
  protected final IModuleProvider moduleProvider = new DefaultModuleProvider();
  protected final IServicesRegistry servicesRegistry = new DefaultServicesRegistry();
  protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

  protected final RPCHandlerRegistry rpcHandlerRegistry = new DefaultRPCHandlerRegistry();
  protected final RPCProviderFactory rpcProviderFactory = new DefaultRPCProviderFactory(
    DefaultObjectMapper.DEFAULT_MAPPER,
    DataBufFactory.defaultFactory());

  protected CloudNetVersion cloudNetVersion;
  protected IPermissionManagement permissionManagement;

  protected INetworkClient networkClient;
  protected CloudServiceFactory cloudServiceFactory;

  protected CloudMessenger messenger;
  protected NodeInfoProvider nodeInfoProvider;
  protected DatabaseProvider databaseProvider;
  protected ServiceTaskProvider serviceTaskProvider;
  protected GroupConfigurationProvider groupConfigurationProvider;
  protected GeneralCloudServiceProvider generalCloudServiceProvider;

  protected DriverEnvironment driverEnvironment = DriverEnvironment.EMBEDDED;

  protected CloudNetDriver(@NotNull List<String> args) {
    this.commandLineArguments = args;
  }

  /**
   * @return a singleton instance of the CloudNetDriver
   */
  @NotNull
  public static CloudNetDriver instance() {
    return CloudNetDriver.instance;
  }

  @VisibleForTesting
  protected static void instance(@NotNull CloudNetDriver instance) {
    CloudNetDriver.instance = instance;
  }

  public abstract void start() throws Exception;

  public abstract void stop();

  /**
   * Returns the name of this component. (e.g. Node-1, Lobby-1)
   */
  @NotNull
  public abstract String componentName();

  /**
   * Gets the component name if it is called on a node and the name of the node that the service is started on if called
   * on the wrapper
   *
   * @return the uniqueId of the current node
   */
  @NotNull
  public abstract String nodeUniqueId();

  /**
   * @return the set {@link CloudServiceFactory} for creating new services in the cloud
   */
  @NotNull
  public CloudServiceFactory cloudServiceFactory() {
    return this.cloudServiceFactory;
  }

  /**
   * @return the set {@link ServiceTaskProvider} for the management of service tasks
   */
  @NotNull
  public ServiceTaskProvider serviceTaskProvider() {
    return this.serviceTaskProvider;
  }

  /**
   * @return the set {@link NodeInfoProvider} which provides access to the local node or nodes in the cluster.
   */
  @NotNull
  public NodeInfoProvider nodeInfoProvider() {
    return this.nodeInfoProvider;
  }

  /**
   * @return the set {@link GroupConfigurationProvider} which manages the services groups
   */
  @NotNull
  public GroupConfigurationProvider groupConfigurationProvider() {
    return this.groupConfigurationProvider;
  }

  /**
   * @return the set {@link CloudMessenger} to communicate between services and nodes
   */
  @NotNull
  public CloudMessenger messenger() {
    return this.messenger;
  }

  /**
   * @return the current {@link IPermissionManagement}
   * @throws NullPointerException if there is no PermissionManagement available
   */
  @NotNull
  public IPermissionManagement permissionManagement() {
    Preconditions.checkNotNull(this.permissionManagement, "no permission management available");
    return this.permissionManagement;
  }

  /**
   * Sets an {@link IPermissionManagement} as current PermissionManagement.
   *
   * @param management the {@link IPermissionManagement} to be set
   * @throws IllegalStateException if the current {@link IPermissionManagement} does not allow overwriting it and the
   *                               class names are not the same
   */
  public void permissionManagement(@NotNull IPermissionManagement management) {
    Preconditions.checkNotNull(management, "new permission management is null");
    // if there is no old permission management or the old permission management can be overridden
    // we can just set the new one
    if (this.permissionManagement == null || this.permissionManagement.canBeOverwritten()) {
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
  @NotNull
  public abstract TemplateStorage localTemplateStorage();

  /**
   * @param storage the name of the storage
   * @return the registered {@link TemplateStorage}, null if the storage does not exist
   */
  @Nullable
  public abstract TemplateStorage templateStorage(@NotNull String storage);

  @NotNull
  public abstract Collection<TemplateStorage> availableTemplateStorages();

  @NotNull
  public ITask<Collection<TemplateStorage>> availableTemplateStoragesAsync() {
    return CompletableTask.supply(this::availableTemplateStorages);
  }

  public @NotNull DatabaseProvider databaseProvider() {
    return this.databaseProvider;
  }

  @NotNull
  public GeneralCloudServiceProvider cloudServiceProvider() {
    return this.generalCloudServiceProvider;
  }

  public @NotNull INetworkClient networkClient() {
    return this.networkClient;
  }

  public abstract @NotNull Collection<String> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId,
    @NotNull String commandLine);

  @NotNull
  public ITask<Collection<String>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId,
    @NotNull String commandLine
  ) {
    return CompletableTask.supply(() -> this.sendCommandLineAsPermissionUser(uniqueId, commandLine));
  }

  /**
   * Fetches the PID of this process.
   *
   * @return the PID as an int or -1, if it couldn't be fetched
   */
  public long ownPID() {
    return ProcessSnapshot.ownPID();
  }

  @NotNull
  public IServicesRegistry servicesRegistry() {
    return this.servicesRegistry;
  }

  @NotNull
  public IEventManager eventManager() {
    return this.eventManager;
  }

  @NotNull
  public IModuleProvider moduleProvider() {
    return this.moduleProvider;
  }

  @NotNull
  public ScheduledExecutorService taskExecutor() {
    return this.scheduler;
  }

  @NotNull
  public RPCProviderFactory rpcProviderFactory() {
    return this.rpcProviderFactory;
  }

  public @NotNull RPCHandlerRegistry rpcHandlerRegistry() {
    return this.rpcHandlerRegistry;
  }

  @NotNull
  public DriverEnvironment environment() {
    return this.driverEnvironment;
  }

  @NotNull
  public CloudNetVersion version() {
    return this.cloudNetVersion;
  }

  @UnmodifiableView
  public @NotNull List<String> commandLineArguments() {
    return Collections.unmodifiableList(this.commandLineArguments);
  }
}
