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

package de.dytanic.cloudnet.ext.bridge.node;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.ProxyFallback;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.listener.TaskConfigListener;
import de.dytanic.cloudnet.ext.bridge.node.command.CommandBridge;
import de.dytanic.cloudnet.ext.bridge.node.command.CommandPlayers;
import de.dytanic.cloudnet.ext.bridge.node.http.V1BridgeConfigurationHttpHandler;
import de.dytanic.cloudnet.ext.bridge.node.listener.BridgeDefaultConfigurationListener;
import de.dytanic.cloudnet.ext.bridge.node.listener.BridgeServiceListCommandListener;
import de.dytanic.cloudnet.ext.bridge.node.listener.BridgeTaskSetupListener;
import de.dytanic.cloudnet.ext.bridge.node.listener.IncludePluginListener;
import de.dytanic.cloudnet.ext.bridge.node.listener.NetworkListenerRegisterListener;
import de.dytanic.cloudnet.ext.bridge.node.listener.NodeCustomChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.node.listener.PlayerManagerListener;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public final class CloudNetBridgeModule extends NodeCloudNetModule {

  private static CloudNetBridgeModule instance;
  private final NodePlayerManager nodePlayerManager = new NodePlayerManager("cloudnet_cloud_players");
  private BridgeConfiguration bridgeConfiguration;

  public CloudNetBridgeModule() {
    instance = this;
  }

  public static CloudNetBridgeModule getInstance() {
    return CloudNetBridgeModule.instance;
  }

  @ModuleTask(order = 64, event = ModuleLifeCycle.STARTED)
  public void createConfiguration() {
    FileUtils.createDirectoryReported(this.getModuleWrapper().getDataDirectory());

    this.bridgeConfiguration = this.getConfig().get("config", BridgeConfiguration.TYPE, new BridgeConfiguration());
    for (Map.Entry<String, String> entry : BridgeConfiguration.DEFAULT_MESSAGES.entrySet()) {
      if (!this.bridgeConfiguration.getMessages().containsKey(entry.getKey())) {
        this.bridgeConfiguration.getMessages().put(entry.getKey(), entry.getValue());
      }
    }

    this.getConfig().append("config", this.bridgeConfiguration);
    this.saveConfig();
  }

  public ProxyFallbackConfiguration createDefaultFallbackConfiguration(String targetGroup) {
    return new ProxyFallbackConfiguration(
      targetGroup,
      "Lobby",
      Collections.singletonList(new ProxyFallback("Lobby", null, 1))
    );
  }

  public void writeConfiguration(BridgeConfiguration bridgeConfiguration) {
    this.getConfig().append("config", bridgeConfiguration);
    this.saveConfig();
  }

  @ModuleTask(order = 36, event = ModuleLifeCycle.STARTED)
  public void initNodePlayerManager() {
    super.getCloudNet().getServicesRegistry()
      .registerService(IPlayerManager.class, "NodePlayerManager", this.nodePlayerManager);

    this.registerListener(new PlayerManagerListener(this.nodePlayerManager));
  }

  @ModuleTask(order = 35, event = ModuleLifeCycle.STARTED)
  public void registerHandlers() {
    this.getHttpServer().registerHandler("/api/v1/modules/bridge/config",
      new V1BridgeConfigurationHttpHandler("cloudnet.http.v1.modules.bridge.config"));
  }

  @ModuleTask(order = 17, event = ModuleLifeCycle.STARTED)
  public void checkTaskConfigurations() {
    // adding a required join permission option to all minecraft-server-based tasks, if not existing
    this.getCloudNet().getServiceTaskProvider().getPermanentServiceTasks().forEach(serviceTask -> {
      if (serviceTask.getProcessConfiguration().getEnvironment().isMinecraftServer() && !serviceTask.getProperties()
        .contains("requiredPermission")) {
        serviceTask.getProperties().appendNull("requiredPermission");
        this.getCloudNet().getServiceTaskProvider().addPermanentServiceTask(serviceTask);
      }
    });
  }

  @ModuleTask(order = 16, event = ModuleLifeCycle.STARTED)
  public void registerCommands() {
    this.registerCommand(new CommandBridge(this));
    this.registerCommand(new CommandPlayers(this.nodePlayerManager));
  }

  @ModuleTask(order = 8, event = ModuleLifeCycle.STARTED)
  public void initListeners() {
    this.registerListeners(new NetworkListenerRegisterListener(), new BridgeTaskSetupListener(),
      new IncludePluginListener(),
      new NodeCustomChannelMessageListener(this.nodePlayerManager), new BridgeDefaultConfigurationListener(),
      new BridgeServiceListCommandListener(), new TaskConfigListener());
  }

  @Override
  public JsonDocument reloadConfig() {
    Path configFile = this.getModuleWrapper().getDataDirectory().resolve("config.json");
    if (Files.notExists(configFile)) {
      this.createConfiguration();
    }

    return super.config = JsonDocument.newDocument(configFile);
  }

  public BridgeConfiguration getBridgeConfiguration() {
    return this.bridgeConfiguration;
  }

  public void setBridgeConfiguration(BridgeConfiguration bridgeConfiguration) {
    this.bridgeConfiguration = bridgeConfiguration;
  }

}
