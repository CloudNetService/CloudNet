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

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.modules.syncproxy.SyncProxyManagement;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.modules.syncproxy.node.command.SyncProxyCommand;
import eu.cloudnetservice.modules.syncproxy.node.listener.NodeSyncProxyChannelMessageListener;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.module.listener.PluginIncludeListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.file.Files;
import lombok.NonNull;

@Singleton
public final class CloudNetSyncProxyModule extends DriverModule {

  @Inject
  public CloudNetSyncProxyModule(@NonNull InjectionLayer<?> layer) {
    layer.installAutoConfigureBindings(this.getClass().getClassLoader(), "syncproxy");
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
  public void initManagement(
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull InjectionLayer<?> injectionLayer
  ) {
    // register the SyncProxyManagement to the ServiceRegistry
    var syncProxyManagement = this.readConfigAndInstantiate(
      injectionLayer,
      SyncProxyConfiguration.class,
      () -> SyncProxyConfiguration.createDefault("Proxy"),
      SyncProxyManagement.class);
    syncProxyManagement.registerService(serviceRegistry);

    // sync the config of the module into the cluster
    dataSyncRegistry.registerHandler(
      DataSyncHandler.<SyncProxyConfiguration>builder()
        .key("syncproxy-config")
        .nameExtractor($ -> "SyncProxy Config")
        .convertObject(SyncProxyConfiguration.class)
        .writer(syncProxyManagement::configuration)
        .singletonCollector(syncProxyManagement::configuration)
        .currentGetter($ -> syncProxyManagement.configuration())
        .build());
  }

  @ModuleTask(order = 64, event = ModuleLifeCycle.LOADED)
  public void initListeners(
    @NonNull EventManager eventManager,
    @NonNull ModuleHelper moduleHelper,
    @NonNull NodeSyncProxyManagement syncProxyManagement
  ) {
    // register the listeners
    eventManager.registerListener(NodeSyncProxyChannelMessageListener.class);
    eventManager.registerListener(new PluginIncludeListener(
      "cloudnet-syncproxy",
      CloudNetSyncProxyModule.class,
      moduleHelper,
      service -> ServiceEnvironmentType.minecraftProxy(service.serviceId().environment())
        && (syncProxyManagement.configuration().loginConfigurations().stream()
        .anyMatch(config -> service.serviceConfiguration().groups().contains(config.targetGroup()))
        || syncProxyManagement.configuration().tabListConfigurations().stream()
        .anyMatch(config -> service.serviceConfiguration().groups().contains(config.targetGroup())))
    ));
  }

  @ModuleTask(order = 60, event = ModuleLifeCycle.LOADED)
  public void registerCommands(@NonNull CommandProvider commandProvider) {
    // register the syncproxy command to provide config management
    commandProvider.register(SyncProxyCommand.class);
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload(@NonNull ServiceRegistry serviceRegistry) {
    var management = serviceRegistry.firstProvider(SyncProxyManagement.class);
    if (management != null) {
      management.configuration(this.loadConfiguration());
    }
  }

  private @NonNull SyncProxyConfiguration loadConfiguration() {
    return this.readConfig(SyncProxyConfiguration.class, () -> SyncProxyConfiguration.createDefault("Proxy"));
  }
}
