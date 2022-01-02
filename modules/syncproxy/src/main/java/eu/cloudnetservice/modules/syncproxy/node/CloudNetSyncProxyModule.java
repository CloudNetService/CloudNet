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

package eu.cloudnetservice.modules.syncproxy.node;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.cloudnet.driver.module.ModuleTask;
import eu.cloudnetservice.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.modules.syncproxy.SyncProxyManagement;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.modules.syncproxy.node.command.CommandSyncProxy;
import eu.cloudnetservice.modules.syncproxy.node.listener.IncludePluginListener;
import eu.cloudnetservice.modules.syncproxy.node.listener.NodeSyncProxyChannelMessageListener;
import java.nio.file.Files;
import lombok.NonNull;

public final class CloudNetSyncProxyModule extends DriverModule {

  private static CloudNetSyncProxyModule instance;

  private NodeSyncProxyManagement nodeSyncProxyManagement;

  public CloudNetSyncProxyModule() {
    instance = this;
  }

  public static CloudNetSyncProxyModule instance() {
    return CloudNetSyncProxyModule.instance;
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
  public void convertConfig() {
    if (Files.exists(this.configPath())) {
      // the old config is located in a document with the key "config", extract the actual config
      var document = this.readConfig().getDocument("config", null);
      // check if there is an old config
      if (document != null) {
        // write the extracted part to the file
        this.writeConfig(document);
      }
    }
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.LOADED)
  public void initManagement() {
    this.nodeSyncProxyManagement = new NodeSyncProxyManagement(this, this.loadConfiguration(), this.rpcFactory());
    // register the SyncProxyManagement to the ServiceRegistry
    this.nodeSyncProxyManagement.registerService(this.serviceRegistry());
    // sync the config of the module into the cluster
    CloudNet.instance().dataSyncRegistry().registerHandler(
      DataSyncHandler.<SyncProxyConfiguration>builder()
        .key("syncproxy-config")
        .nameExtractor($ -> "SyncProxy Config")
        .convertObject(SyncProxyConfiguration.class)
        .writer(this.nodeSyncProxyManagement::configuration)
        .singletonCollector(this.nodeSyncProxyManagement::configuration)
        .currentGetter($ -> this.nodeSyncProxyManagement.configuration())
        .build());
  }

  @ModuleTask(order = 64, event = ModuleLifeCycle.LOADED)
  public void initListeners() {
    // register the listeners
    this.registerListener(new IncludePluginListener(this.nodeSyncProxyManagement),
      new NodeSyncProxyChannelMessageListener(this.nodeSyncProxyManagement, this.eventManager()));
  }

  @ModuleTask(order = 60, event = ModuleLifeCycle.LOADED)
  public void registerCommands() {
    // register the syncproxy command to provide config management
    CloudNet.instance().commandProvider().register(new CommandSyncProxy(this.nodeSyncProxyManagement));
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    var management = this.serviceRegistry().firstService(SyncProxyManagement.class);
    if (management != null) {
      management.configuration(this.loadConfiguration());
    }
  }

  private @NonNull SyncProxyConfiguration loadConfiguration() {
    // read the config from the file
    var configuration = this.readConfig().toInstanceOf(SyncProxyConfiguration.class);
    // check if we need to create a default config
    if (configuration == null || Files.notExists(this.configPath())) {
      // create default config and write to the file
      this.writeConfig(JsonDocument.newDocument(configuration = SyncProxyConfiguration.createDefault("Proxy")));
    }
    return configuration;
  }
}
