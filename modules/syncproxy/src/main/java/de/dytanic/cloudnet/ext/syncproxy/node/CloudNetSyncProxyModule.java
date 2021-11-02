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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.sync.DataSyncHandler;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfigurationHelper;
import de.dytanic.cloudnet.ext.syncproxy.node.command.CommandSyncProxy;
import de.dytanic.cloudnet.ext.syncproxy.node.listener.IncludePluginListener;
import de.dytanic.cloudnet.ext.syncproxy.node.listener.SyncProxyDefaultConfigurationListener;
import java.nio.file.Path;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

public final class CloudNetSyncProxyModule extends DriverModule {

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
  public void initClusterConfiguration() {
    CloudNet.getInstance().getDataSyncRegistry().registerHandler(
      DataSyncHandler.<SyncProxyConfiguration>builder()
        .key("syncproxy-config")
        .nameExtractor(cloudPermissionsConfig -> "SyncProxy Config")
        .convertObject(SyncProxyConfiguration.class)
        .writer(cloudPermissionConfig -> SyncProxyConfigurationHelper.write(cloudPermissionConfig,
          this.configurationFilePath))
        .dataCollector(() -> Collections.singleton(this.syncProxyConfiguration))
        .currentGetter($ -> this.syncProxyConfiguration)
        .build());
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
  public void initConfig() {
    this.configurationFilePath = this.moduleWrapper.getDataDirectory().resolve("config.json");
    this.syncProxyConfiguration = SyncProxyConfigurationHelper.read(this.configurationFilePath);
  }

  @ModuleTask(order = 64, event = ModuleLifeCycle.STARTED)
  public void initListeners() {
    this.registerListeners(new IncludePluginListener(), new SyncProxyDefaultConfigurationListener(this));
  }

  @ModuleTask(order = 60, event = ModuleLifeCycle.STARTED)
  public void registerCommands() {
    CloudNet.getInstance().getCommandProvider().register(new CommandSyncProxy(this));
  }

  @NotNull
  public SyncProxyConfiguration getSyncProxyConfiguration() {
    return this.syncProxyConfiguration;
  }

  public void setSyncProxyConfiguration(@NotNull SyncProxyConfiguration syncProxyConfiguration) {
    this.setSyncProxyConfiguration(syncProxyConfiguration, false);
  }

  public void setSyncProxyConfiguration(@NotNull SyncProxyConfiguration syncProxyConfiguration, boolean clusterUpdate) {
    this.syncProxyConfiguration = syncProxyConfiguration;
    if (clusterUpdate) {
      ChannelMessage.builder()
        .channel(SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME)
        .message(SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION)
        .targetAll()
        .buffer(DataBuf.empty().writeObject(syncProxyConfiguration))
        .build()
        .send();
    }

    SyncProxyConfigurationHelper.write(syncProxyConfiguration, this.configurationFilePath);
  }

  @NotNull
  public Path getConfigurationFilePath() {
    return this.configurationFilePath;
  }
}
