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

import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.syncproxy.SyncProxyManagement;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.modules.syncproxy.node.command.SyncProxyCommand;
import eu.cloudnetservice.modules.syncproxy.node.listener.NodeSyncProxyChannelMessageListener;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.module.listener.PluginIncludeListener;
import java.nio.file.Files;
import lombok.NonNull;

public final class CloudNetSyncProxyModule extends DriverModule {

  private final Node node = Node.instance();

  private NodeSyncProxyManagement nodeSyncProxyManagement;

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
    this.node.dataSyncRegistry().registerHandler(
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
    this.registerListener(new NodeSyncProxyChannelMessageListener(this.nodeSyncProxyManagement, this.eventManager()));
    this.registerListener(new PluginIncludeListener(
      "cloudnet-syncproxy",
      CloudNetSyncProxyModule.class,
      service -> ServiceEnvironmentType.minecraftProxy(service.serviceId().environment())
        && (this.nodeSyncProxyManagement.configuration().loginConfigurations().stream()
        .anyMatch(config -> service.serviceConfiguration().groups().contains(config.targetGroup()))
        || this.nodeSyncProxyManagement.configuration().tabListConfigurations().stream()
        .anyMatch(config -> service.serviceConfiguration().groups().contains(config.targetGroup())))
    ));
  }

  @ModuleTask(order = 60, event = ModuleLifeCycle.LOADED)
  public void registerCommands() {
    // register the syncproxy command to provide config management
    Node.instance().commandProvider().register(new SyncProxyCommand(this.nodeSyncProxyManagement, this.node));
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    var management = this.serviceRegistry().firstProvider(SyncProxyManagement.class);
    if (management != null) {
      management.configuration(this.loadConfiguration());
    }
  }

  private @NonNull SyncProxyConfiguration loadConfiguration() {
    return this.readConfig(SyncProxyConfiguration.class, () -> SyncProxyConfiguration.createDefault("Proxy"));
  }
}
