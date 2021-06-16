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

package de.dytanic.cloudnet.ext.syncproxy.node;

import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfigurationWriterAndReader;
import de.dytanic.cloudnet.ext.syncproxy.node.command.CommandSyncProxy;
import de.dytanic.cloudnet.ext.syncproxy.node.http.V1SyncProxyConfigurationHttpHandler;
import de.dytanic.cloudnet.ext.syncproxy.node.listener.IncludePluginListener;
import de.dytanic.cloudnet.ext.syncproxy.node.listener.SyncProxyConfigUpdateListener;
import de.dytanic.cloudnet.ext.syncproxy.node.listener.SyncProxyDefaultConfigurationListener;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import java.io.File;
import java.nio.file.Path;

public final class CloudNetSyncProxyModule extends NodeCloudNetModule {

  private static CloudNetSyncProxyModule instance;

  private Path configurationFilePath;
  private SyncProxyConfiguration syncProxyConfiguration;

  public CloudNetSyncProxyModule() {
    instance = this;
  }

  public static CloudNetSyncProxyModule getInstance() {
    return CloudNetSyncProxyModule.instance;
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void createConfigurationOrUpdate() {
    this.configurationFilePath = this.getModuleWrapper().getDataDirectory().resolve("config.json");
    this.syncProxyConfiguration = SyncProxyConfigurationWriterAndReader.read(this.configurationFilePath);
  }

  @ModuleTask(order = 64, event = ModuleLifeCycle.STARTED)
  public void initListeners() {
    this.registerListeners(new IncludePluginListener(), new SyncProxyConfigUpdateListener(),
      new SyncProxyDefaultConfigurationListener());
  }

  @ModuleTask(order = 60, event = ModuleLifeCycle.STARTED)
  public void registerCommands() {
    this.registerCommand(new CommandSyncProxy(this));
  }

  @ModuleTask(order = 35, event = ModuleLifeCycle.STARTED)
  public void registerHttpHandlers() {
    this.getCloudNet().getHttpServer().registerHandler("/api/v1/modules/syncproxy/config",
      new V1SyncProxyConfigurationHttpHandler("cloudnet.http.v1.modules.syncproxy.config"));
  }

  public SyncProxyConfiguration getSyncProxyConfiguration() {
    return this.syncProxyConfiguration;
  }

  public void setSyncProxyConfiguration(SyncProxyConfiguration syncProxyConfiguration) {
    this.syncProxyConfiguration = syncProxyConfiguration;
  }

  @Deprecated
  public File getConfigurationFile() {
    return this.configurationFilePath.toFile();
  }

  public Path getConfigurationFilePath() {
    return this.configurationFilePath;
  }
}
