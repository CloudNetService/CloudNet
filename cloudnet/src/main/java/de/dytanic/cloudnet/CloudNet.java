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

package de.dytanic.cloudnet;

import aerogel.Inject;
import aerogel.Singleton;
import aerogel.auto.Provides;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.conf.IConfiguration;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.provider.NodeInfoProvider;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the implementation of the {@link CloudNetDriver} for nodes.
 */
@Singleton
@Provides(CloudNetDriver.class)
public final class CloudNet extends CloudNetDriver {

  private final IConfiguration configuration;
  private final INetworkClient networkClient;
  private final IClusterNodeServerProvider nodeServerProvider;

  @Inject
  public CloudNet(
    IConfiguration configuration,
    IClusterNodeServerProvider nodeServerProvider,
    CloudMessenger messenger,
    NodeInfoProvider nodeInfoProvider,
    CloudServiceFactory cloudServiceFactory,
    ServiceTaskProvider serviceTaskProvider,
    IPermissionManagement permissionManagement,
    GroupConfigurationProvider groupConfigurationProvider,
    ICloudServiceManager generalCloudServiceProvider
  ) {
    setInstance(this);

    this.configuration = configuration;
    this.nodeServerProvider = nodeServerProvider;
    this.messenger = messenger;
    this.nodeInfoProvider = nodeInfoProvider;
    this.cloudServiceFactory = cloudServiceFactory;
    this.serviceTaskProvider = serviceTaskProvider;
    this.permissionManagement = permissionManagement;
    this.groupConfigurationProvider = groupConfigurationProvider;
    this.generalCloudServiceProvider = generalCloudServiceProvider;

    this.networkClient = null;
    this.driverEnvironment = DriverEnvironment.CLOUDNET;
  }

  public static @NotNull CloudNet getInstance() {
    return (CloudNet) CloudNetDriver.getInstance();
  }

  @Override
  public void start() throws Exception {

  }

  @Override
  public void stop() {

  }

  @Override
  public @NotNull String getComponentName() {
    return this.configuration.getIdentity().getUniqueId();
  }

  @Override
  public @NotNull String getNodeUniqueId() {
    return this.configuration.getIdentity().getUniqueId();
  }

  @Override
  public @NotNull TemplateStorage getLocalTemplateStorage() {
    TemplateStorage localStorage = this.getTemplateStorage(ServiceTemplate.LOCAL_STORAGE);
    if (localStorage == null) {
      // this should never happen
      throw new UnsupportedOperationException("Local template storage is not present");
    }

    return localStorage;
  }

  @Override
  public @Nullable TemplateStorage getTemplateStorage(String storage) {
    return this.servicesRegistry.getService(TemplateStorage.class, storage);
  }

  @Override
  public @NotNull Collection<TemplateStorage> getAvailableTemplateStorages() {
    return this.servicesRegistry.getServices(TemplateStorage.class);
  }

  @Override
  public @NotNull AbstractDatabaseProvider getDatabaseProvider() {
    return this.servicesRegistry.getFirstService(AbstractDatabaseProvider.class);
  }

  @Override
  public @NotNull INetworkClient getNetworkClient() {
    return this.networkClient;
  }

  @Override
  public Pair<Boolean, String[]> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId, @NotNull String commandLine) {
    // TODO: re-implement when the commands are back
    return null;
  }

  @Override
  public @NotNull ICloudServiceManager getCloudServiceProvider() {
    return (ICloudServiceManager) super.getCloudServiceProvider();
  }

  public @NotNull IConfiguration getConfig() {
    return this.configuration;
  }

  public @NotNull IClusterNodeServerProvider getClusterNodeServerProvider() {
    return this.nodeServerProvider;
  }
}
