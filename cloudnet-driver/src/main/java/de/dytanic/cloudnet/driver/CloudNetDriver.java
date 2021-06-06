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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CloudNetDriver {

  private static CloudNetDriver instance;
  protected final IServicesRegistry servicesRegistry = new DefaultServicesRegistry();
  protected final IEventManager eventManager = new DefaultEventManager();
  protected final IModuleProvider moduleProvider = new DefaultModuleProvider();
  protected final ITaskScheduler taskScheduler = new DefaultTaskScheduler();
  protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
  protected final ILogger logger;
  protected IPermissionManagement permissionManagement;
  protected CloudServiceFactory cloudServiceFactory;
  protected GeneralCloudServiceProvider generalCloudServiceProvider;
  protected ServiceTaskProvider serviceTaskProvider;
  protected GroupConfigurationProvider groupConfigurationProvider;
  protected NodeInfoProvider nodeInfoProvider;
  protected CloudMessenger messenger;
  protected DriverEnvironment driverEnvironment = DriverEnvironment.EMBEDDED;

  public CloudNetDriver(@NotNull ILogger logger) {
    this.logger = logger;
  }

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
   */
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
   * Gets the component name if it is called on a node and the name of the node if it is called on a wrapper.
   *
   * @return the uniqueId of the current node
   */
  @NotNull
  public abstract String getNodeUniqueId();

  @NotNull
  public CloudServiceFactory getCloudServiceFactory() {
    return this.cloudServiceFactory;
  }

  @NotNull
  public ServiceTaskProvider getServiceTaskProvider() {
    return this.serviceTaskProvider;
  }

  @NotNull
  public NodeInfoProvider getNodeInfoProvider() {
    return this.nodeInfoProvider;
  }

  @NotNull
  public GroupConfigurationProvider getGroupConfigurationProvider() {
    return this.groupConfigurationProvider;
  }

  @NotNull
  public CloudMessenger getMessenger() {
    return this.messenger;
  }

  @NotNull
  public IPermissionManagement getPermissionManagement() {
    Preconditions.checkNotNull(this.permissionManagement, "no permission management available");
    return this.permissionManagement;
  }

  public void setPermissionManagement(@NotNull IPermissionManagement permissionManagement) {
    if (this.permissionManagement != null && !this.permissionManagement.canBeOverwritten() && !this.permissionManagement
      .getClass().getName().equals(permissionManagement.getClass().getName())) {
      throw new IllegalStateException("Current permission management (" + this.permissionManagement.getClass().getName()
        + ") cannot be overwritten by " + permissionManagement.getClass().getName());
    }

    this.permissionManagement = permissionManagement;
  }

  @NotNull
  public abstract TemplateStorage getLocalTemplateStorage();

  @Nullable
  public abstract TemplateStorage getTemplateStorage(String storage);

  @NotNull
  public abstract Collection<TemplateStorage> getAvailableTemplateStorages();

  @NotNull
  public abstract ITask<Collection<TemplateStorage>> getAvailableTemplateStoragesAsync();

  @NotNull
  public abstract DatabaseProvider getDatabaseProvider();

  /**
   * Returns a new service specific CloudServiceProvider
   *
   * @param name the name of the service
   * @return the new instance of the {@link SpecificCloudServiceProvider}
   */
  @NotNull
  public abstract SpecificCloudServiceProvider getCloudServiceProvider(@NotNull String name);

  /**
   * Returns a new service specific CloudServiceProvider
   *
   * @param uniqueId the uniqueId of the service
   * @return the new instance of the {@link SpecificCloudServiceProvider}
   */
  @NotNull
  public abstract SpecificCloudServiceProvider getCloudServiceProvider(@NotNull UUID uniqueId);

  /**
   * Returns a new service specific CloudServiceProvider
   *
   * @param serviceInfoSnapshot the info of the service to create a provider for
   * @return the new instance of the {@link SpecificCloudServiceProvider}
   */
  @NotNull
  public abstract SpecificCloudServiceProvider getCloudServiceProvider(
    @NotNull ServiceInfoSnapshot serviceInfoSnapshot);

  /**
   * Returns the general CloudServiceProvider
   *
   * @return the instance of the {@link GeneralCloudServiceProvider}
   */
  @NotNull
  public GeneralCloudServiceProvider getCloudServiceProvider() {
    return this.generalCloudServiceProvider;
  }

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

  public abstract void setGlobalLogLevel(@NotNull LogLevel logLevel);

  public abstract void setGlobalLogLevel(int logLevel);

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

  @NotNull
  public IServicesRegistry getServicesRegistry() {
    return this.servicesRegistry;
  }

  @NotNull
  public IEventManager getEventManager() {
    return this.eventManager;
  }

  @NotNull
  public IModuleProvider getModuleProvider() {
    return this.moduleProvider;
  }

  @NotNull
  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public ITaskScheduler getTaskScheduler() {
    return this.taskScheduler;
  }

  @NotNull
  public ScheduledExecutorService getTaskExecutor() {
    return this.scheduler;
  }

  @NotNull
  public ILogger getLogger() {
    return this.logger;
  }

  @NotNull
  public DriverEnvironment getDriverEnvironment() {
    return this.driverEnvironment;
  }

}
