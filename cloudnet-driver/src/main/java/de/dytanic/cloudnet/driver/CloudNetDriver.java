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
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.DefaultTaskScheduler;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskScheduler;
import de.dytanic.cloudnet.common.logging.DefaultAsyncLogger;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.common.registry.DefaultServicesRegistry;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.database.DatabaseProvider;
import de.dytanic.cloudnet.driver.event.DefaultEventManager;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.module.DefaultModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.defaults.DefaultRPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.provider.NodeInfoProvider;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CloudNetDriver {

  private static CloudNetDriver instance;

  protected final IEventManager eventManager = new DefaultEventManager();
  protected final IModuleProvider moduleProvider = new DefaultModuleProvider();
  protected final IServicesRegistry servicesRegistry = new DefaultServicesRegistry();
  protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
  protected final RPCProviderFactory rpcProviderFactory = new DefaultRPCProviderFactory(new DefaultObjectMapper(),
    DataBufFactory.defaultFactory());

  @Deprecated
  protected final ITaskScheduler taskScheduler = new DefaultTaskScheduler();
  @Deprecated
  protected final ILogger iLogger;

  protected CloudMessenger messenger;
  protected NodeInfoProvider nodeInfoProvider;
  protected CloudServiceFactory cloudServiceFactory;
  protected ServiceTaskProvider serviceTaskProvider;
  protected IPermissionManagement permissionManagement;
  protected GroupConfigurationProvider groupConfigurationProvider;
  protected GeneralCloudServiceProvider generalCloudServiceProvider;

  protected DriverEnvironment driverEnvironment = DriverEnvironment.EMBEDDED;

  public CloudNetDriver() {
    this.iLogger = new DefaultAsyncLogger();
  }

  @Deprecated
  @ScheduledForRemoval
  public CloudNetDriver(@NotNull ILogger iLogger) {
    this.iLogger = iLogger;
  }

  /**
   * @return a singleton instance of the CloudNetDriver
   */
  @NotNull
  public static CloudNetDriver getInstance() {
    return CloudNetDriver.instance;
  }

  protected static void setInstance(@NotNull CloudNetDriver instance) {
    CloudNetDriver.instance = instance;
  }

  /**
   * The CloudNetDriver instance won't be null usually, this method is only relevant for tests
   *
   * @return optional CloudNetDriver
   * @deprecated In the runtime the driver instance is always present, use {@link #getInstance()} instead.
   */
  @Deprecated
  @ScheduledForRemoval
  public static Optional<CloudNetDriver> optionalInstance() {
    return Optional.ofNullable(CloudNetDriver.instance);
  }

  public abstract void start() throws Exception;

  public abstract void stop();


  /**
   * Returns the name of this component. (e.g. Node-1, Lobby-1)
   */
  @NotNull
  public abstract String getComponentName();

  /**
   * Gets the component name if it is called on a node and the name of the node that the service is started on if called
   * on the wrapper
   *
   * @return the uniqueId of the current node
   */
  @NotNull
  public abstract String getNodeUniqueId();

  /**
   * @return the set {@link CloudServiceFactory} for creating new services in the cloud
   */
  @NotNull
  public CloudServiceFactory getCloudServiceFactory() {
    return this.cloudServiceFactory;
  }

  /**
   * @return the set {@link ServiceTaskProvider} for the management of service tasks
   */
  @NotNull
  public ServiceTaskProvider getServiceTaskProvider() {
    return this.serviceTaskProvider;
  }

  /**
   * @return the set {@link NodeInfoProvider} which provides access to the local node or nodes in the cluster.
   */
  @NotNull
  public NodeInfoProvider getNodeInfoProvider() {
    return this.nodeInfoProvider;
  }

  /**
   * @return the set {@link GroupConfigurationProvider} which manages the services groups
   */
  @NotNull
  public GroupConfigurationProvider getGroupConfigurationProvider() {
    return this.groupConfigurationProvider;
  }

  /**
   * @return the set {@link CloudMessenger} to communicate between services and nodes
   */
  @NotNull
  public CloudMessenger getMessenger() {
    return this.messenger;
  }

  /**
   * @return the current {@link IPermissionManagement}
   * @throws NullPointerException if there is no PermissionManagement available
   */
  @NotNull
  public IPermissionManagement getPermissionManagement() {
    Preconditions.checkNotNull(this.permissionManagement, "no permission management available");
    return this.permissionManagement;
  }

  /**
   * Sets an {@link IPermissionManagement} as current PermissionManagement.
   *
   * @param permissionManagement the {@link IPermissionManagement} to be set
   * @throws IllegalStateException if the current {@link IPermissionManagement} does not allow overwriting it and the
   *                               class names are not the same
   */
  public void setPermissionManagement(@NotNull IPermissionManagement permissionManagement) {
    Preconditions.checkNotNull(permissionManagement, "new permission management is null");

    if (this.permissionManagement != null && !this.permissionManagement.canBeOverwritten() && !this.permissionManagement
      .getClass().getName().equals(permissionManagement.getClass().getName())) {
      throw new IllegalStateException("Current permission management (" + this.permissionManagement.getClass().getName()
        + ") cannot be overwritten by " + permissionManagement.getClass().getName());
    }

    this.permissionManagement = permissionManagement;
  }

  /**
   * Returns the local {@link TemplateStorage} of the Node.
   *
   * @return the local {@link TemplateStorage}
   * @throws IllegalStateException if the TemplateStorage is not present
   */
  @NotNull
  public abstract TemplateStorage getLocalTemplateStorage();

  /**
   * @param storage the name of the storage
   * @return the registered {@link TemplateStorage}, null if the storage does not exist
   */
  @Nullable
  public abstract TemplateStorage getTemplateStorage(String storage);

  /**
   * This method retrieves all {@link TemplateStorage} from the Nodes {@link IServicesRegistry}
   *
   * @return a Collection with all available TemplatesStorages
   */
  @NotNull
  public abstract Collection<TemplateStorage> getAvailableTemplateStorages();

  /**
   * This method retrieves all {@link TemplateStorage} from the Nodes {@link IServicesRegistry} async
   *
   * @return a Collection with all available TemplatesStorages
   */
  @NotNull
  public abstract ITask<Collection<TemplateStorage>> getAvailableTemplateStoragesAsync();

  /**
   * @return the active DatabaseProvider specified by the local/registry
   */
  @NotNull
  public abstract DatabaseProvider getDatabaseProvider();

  /**
   * Returns a new service specific CloudServiceProvider
   *
   * @param name the name of the service
   * @return the new instance of the {@link SpecificCloudServiceProvider}
   */
  @NotNull
  public abstract SpecificCloudServiceProvider getCloudServiceProvider(@NotNull String name); // todo: deprecate

  /**
   * Returns a new service specific CloudServiceProvider
   *
   * @param uniqueId the uniqueId of the service
   * @return the new instance of the {@link SpecificCloudServiceProvider}
   */
  @NotNull
  public abstract SpecificCloudServiceProvider getCloudServiceProvider(@NotNull UUID uniqueId); // todo: deprecate

  /**
   * Returns a new service specific CloudServiceProvider
   *
   * @param serviceInfoSnapshot the info of the service to create a provider for
   * @return the new instance of the {@link SpecificCloudServiceProvider}
   */
  @NotNull
  public abstract SpecificCloudServiceProvider getCloudServiceProvider(
    @NotNull ServiceInfoSnapshot serviceInfoSnapshot); // todo: deprecate

  /**
   * Returns the general CloudServiceProvider
   *
   * @return the instance of the {@link GeneralCloudServiceProvider}
   */
  @NotNull
  public GeneralCloudServiceProvider getCloudServiceProvider() {
    return this.generalCloudServiceProvider;
  }

  /**
   * @return the network client used for communication
   */
  @NotNull
  public abstract INetworkClient getNetworkClient();

  /**
   * @deprecated use {@link #getLocalTemplateStorage()} instead
   */
  @Deprecated
  @NotNull
  public ITask<Collection<ServiceTemplate>> getLocalTemplateStorageTemplatesAsync() {
    return this.getLocalTemplateStorage().getTemplatesAsync();
  }

  /**
   * @deprecated use {@link #getTemplateStorage(String)} instead
   */
  @Deprecated
  @NotNull
  public ITask<Collection<ServiceTemplate>> getTemplateStorageTemplatesAsync(@NotNull String serviceName) {
    TemplateStorage storage = this.getTemplateStorage(serviceName);
    return storage != null ? storage.getTemplatesAsync() : CompletedTask.create(Collections.emptyList());
  }

  /**
   * @deprecated use {@link #getLocalTemplateStorage()} instead
   */
  @Deprecated
  @NotNull
  public Collection<ServiceTemplate> getLocalTemplateStorageTemplates() {
    return this.getLocalTemplateStorage().getTemplates();
  }

  /**
   * @deprecated use {@link #getTemplateStorage(String)} instead
   */
  @Deprecated
  @NotNull
  public Collection<ServiceTemplate> getTemplateStorageTemplates(@NotNull String serviceName) {
    TemplateStorage storage = this.getTemplateStorage(serviceName);
    return storage != null ? storage.getTemplates() : Collections.emptyList();
  }

  @Deprecated
  @ScheduledForRemoval
  public void setGlobalLogLevel(@NotNull LogLevel logLevel) {
    this.setGlobalLogLevel(logLevel.getLevel());
  }

  @Deprecated
  @ScheduledForRemoval
  public void setGlobalLogLevel(int logLevel) {
    switch (logLevel) {
      case 0:
      case 1:
        this.setGlobalLogLevel(Level.INFO);
        break;
      case 125:
        this.setGlobalLogLevel(Level.WARNING);
        break;
      case 126:
      case 127:
        this.setGlobalLogLevel(Level.SEVERE);
        break;
      case 128:
      case 129:
        this.setGlobalLogLevel(Level.FINE);
        break;
      case Integer.MAX_VALUE:
        this.setGlobalLogLevel(Level.ALL);
        break;
      default:
        break;
    }
  }

  /**
   * Sets the log level for the root logger of the {@link de.dytanic.cloudnet.common.log.LogManager}
   *
   * @param logLevel the {@link Level} to be set as global level
   */
  public abstract void setGlobalLogLevel(Level logLevel);

  public abstract Pair<Boolean, String[]> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId,
    @NotNull String commandLine);

  @NotNull
  public abstract ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId,
    @NotNull String commandLine);

  /**
   * Fetches the PID of this process.
   *
   * @return the PID as an int or -1, if it couldn't be fetched
   */
  public int getOwnPID() {
    return ProcessSnapshot.getOwnPID();
  }

  /**
   * @return the {@link IServicesRegistry} for the management of implementations
   */
  @NotNull
  public IServicesRegistry getServicesRegistry() {
    return this.servicesRegistry;
  }

  /**
   * @return the {@link IEventManager} for event management
   */
  @NotNull
  public IEventManager getEventManager() {
    return this.eventManager;
  }

  /**
   * @return the {@link IModuleProvider} for module management
   */
  @NotNull
  public IModuleProvider getModuleProvider() {
    return this.moduleProvider;
  }

  @NotNull
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  public ITaskScheduler getTaskScheduler() {
    return this.taskScheduler;
  }

  /**
   * Use this {@link ScheduledExecutorService} to schedule actions
   *
   * @return the set {@link ScheduledExecutorService}
   */
  @NotNull
  public ScheduledExecutorService getTaskExecutor() {
    return this.scheduler;
  }

  /**
   * @deprecated Don't use this logger instance - create your own one using a lib or the build in {@link
   * de.dytanic.cloudnet.common.log.LogManager}.
   */
  @NotNull
  @Deprecated
  @ScheduledForRemoval
  public ILogger getLogger() {
    return this.iLogger;
  }

  public RPCProviderFactory getRPCProviderFactory() {
    return this.rpcProviderFactory;
  }

  /**
   * @return the {@link DriverEnvironment} this driver is running on.
   */
  @NotNull
  public DriverEnvironment getDriverEnvironment() {
    return this.driverEnvironment;
  }

}
