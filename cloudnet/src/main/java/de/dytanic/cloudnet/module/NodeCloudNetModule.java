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

package de.dytanic.cloudnet.module;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.conf.IConfiguration;
import de.dytanic.cloudnet.conf.IConfigurationRegistry;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.template.TemplateStorage;

public abstract class NodeCloudNetModule extends DriverModule {

  public final void registerCommand(Command command) {
    this.getCloudNet().getCommandMap().registerCommand(command);
  }

  public final <T extends TemplateStorage> T registerTemplateStorage(String serviceName, T templateStorage) {
    Preconditions.checkNotNull(serviceName);
    Preconditions.checkNotNull(templateStorage);

    this.getRegistry().registerService(TemplateStorage.class, serviceName, templateStorage);

    return templateStorage;
  }

  public final <T extends AbstractDatabaseProvider> T registerDatabaseProvider(String serviceName, T databaseProvider) {
    Preconditions.checkNotNull(serviceName);
    Preconditions.checkNotNull(databaseProvider);

    this.getRegistry().registerService(AbstractDatabaseProvider.class, serviceName, databaseProvider);

    return databaseProvider;
  }

  public final IHttpServer registerHttpHandler(String path, IHttpHandler... httpHandlers) {
    Preconditions.checkNotNull(path);
    Preconditions.checkNotNull(httpHandlers);

    return this.getHttpServer().registerHandler(path, httpHandlers);
  }

  public final AbstractDatabaseProvider getDatabaseProvider() {
    return this.getCloudNet().getDatabaseProvider();
  }

  public final IHttpServer getHttpServer() {
    return this.getCloudNet().getHttpServer();
  }

  public final NetworkClusterNode getIdentity() {
    return this.getCloudNetConfig().getIdentity();
  }

  public final IConfiguration getCloudNetConfig() {
    return CloudNet.getInstance().getConfig();
  }

  public final IConfigurationRegistry getCloudRegistry() {
    return CloudNet.getInstance().getConfigurationRegistry();
  }

  public final CloudNet getCloudNet() {
    return CloudNet.getInstance();
  }
}
