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

package eu.cloudnetservice.modules.bridge.node;

import static eu.cloudnetservice.modules.bridge.BridgeManagement.BRIDGE_PLAYER_DB_NAME;

import com.google.common.collect.Iterables;
import com.google.gson.reflect.TypeToken;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.cloudnet.driver.module.ModuleTask;
import eu.cloudnetservice.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpHandler;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import eu.cloudnetservice.cloudnet.driver.registry.ServiceRegistry;
import eu.cloudnetservice.cloudnet.node.Node;
import eu.cloudnetservice.cloudnet.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.config.BridgeConfiguration;
import eu.cloudnetservice.modules.bridge.config.ProxyFallbackConfiguration;
import eu.cloudnetservice.modules.bridge.node.command.CommandBridge;
import eu.cloudnetservice.modules.bridge.node.http.V2HttpHandlerBridge;
import eu.cloudnetservice.modules.bridge.rpc.ComponentObjectSerializer;
import eu.cloudnetservice.modules.bridge.rpc.TitleObjectSerializer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public final class CloudNetBridgeModule extends DriverModule {

  private static final Logger LOGGER = LogManager.logger(CloudNetBridgeModule.class);

  @ModuleTask(order = 50, event = ModuleLifeCycle.LOADED)
  public void initNetworkHelpers() {
    DefaultObjectMapper.DEFAULT_MAPPER
      .registerBinding(Title.class, new TitleObjectSerializer(), false)
      .registerBinding(Component.class, new ComponentObjectSerializer(), false);
  }

  @ModuleTask(order = 40, event = ModuleLifeCycle.LOADED)
  public void convertOldConfiguration() {
    // read the file
    var config = JsonDocument.newDocument(this.configPath()).getDocument("config");
    // check if the old file exists
    if (!config.empty()) {
      // extract the messages and re-map them
      Map<String, Map<String, String>> messages = new HashMap<>(BridgeConfiguration.DEFAULT_MESSAGES);
      messages.get("default").putAll(config.get("messages", new TypeToken<Map<String, String>>() {
      }.getType()));
      // extract all hub commands
      Collection<String> hubCommands = config.get("hubCommandNames", new TypeToken<Collection<String>>() {
      }.getType());
      // extract the excluded groups
      Collection<String> excludedGroups = config.get("excludedGroups", new TypeToken<Collection<String>>() {
      }.getType());
      // extract the fallback configurations
      Collection<ProxyFallbackConfiguration> fallbacks = config.get(
        "bungeeFallbackConfigurations",
        new TypeToken<Collection<ProxyFallbackConfiguration>>() {
        }.getType());
      // convert to a new config file
      JsonDocument.newDocument(new BridgeConfiguration(
        config.getString("prefix"),
        messages,
        config.getBoolean("logPlayerConnections"),
        excludedGroups,
        hubCommands,
        fallbacks,
        config.getDocument("properties")
      )).write(this.configPath());
    }
  }

  @ModuleTask(event = ModuleLifeCycle.STARTED)
  public void convertOldDatabaseEntries() {
    var playerDb = Node.instance().databaseProvider().database(BRIDGE_PLAYER_DB_NAME);
    // read the first player from the database - if the first player is valid we don't need to take a look at the other
    // players in the database as they were already converted
    var first = playerDb.readChunk(0, 1);
    if (first != null && !first.isEmpty()) {
      var document = Iterables.getOnlyElement(first.values());
      // validate the offline player
      var lastNetworkPlayerProxyInfo = document.getDocument("lastNetworkPlayerProxyInfo");
      // check if the document is empty, if so it indicates an old database format
      if (lastNetworkPlayerProxyInfo.empty()) {
        LOGGER.warning("Converting the offline player database, this may take a few seconds...");

        var convertedPlayers = 0;
        // invalid player data - convert the database
        Map<String, JsonDocument> chunkData;
        while ((chunkData = playerDb.readChunk(convertedPlayers, 100)) != null) {
          for (var entry : chunkData.entrySet()) {
            // get all the required path
            var lastProxyInfo = entry.getValue().getDocument("lastNetworkConnectionInfo");
            var networkService = lastProxyInfo.getDocument("networkService");

            // rewrite the name of the environment
            JsonDocument serviceId = networkService.getDocument("serviceId");
            var environment = serviceId.getString("environment", "");
            serviceId.append("environmentName", environment);
            // try to set the new environment
            var env = Node.instance().serviceVersionProvider()
              .getEnvironmentType(environment)
              .orElse(null);
            serviceId.append("environment", env);

            // rewrite smaller changes
            lastProxyInfo.remove("legacy");
            lastProxyInfo.append("xBoxId", entry.getValue().getString("xBoxId"));

            // rewrite all paths of the document
            networkService.append("serviceId", serviceId);
            lastProxyInfo.append("networkService", networkService);
            entry.getValue().append("lastNetworkPlayerProxyInfo", lastProxyInfo);

            // remove the outdated info
            entry.getValue().remove("xBoxId");
            entry.getValue().remove("uniqueId");
            entry.getValue().remove("lastNetworkConnectionInfo");

            // update the entry
            playerDb.insert(entry.getKey(), entry.getValue());
          }
          // count the converted players up
          convertedPlayers += chunkData.size();
          // check if the chunk size was exactly 100 players - if not we just completed the last chunk
          if (chunkData.size() != 100) {
            break;
          }
        }
        // notify about the completion
        LOGGER.info("Successfully converted %d entries", null, convertedPlayers);
      }
    }
  }

  @ModuleTask(event = ModuleLifeCycle.LOADED)
  public void initModule() {
    // init the bridge management
    BridgeManagement management = new NodeBridgeManagement(
      this,
      this.loadConfiguration(),
      this.eventManager(),
      Node.instance().dataSyncRegistry(),
      this.rpcFactory());
    management.registerServices(this.serviceRegistry());
    management.postInit();
    // register the cluster sync handler
    Node.instance().dataSyncRegistry().registerHandler(DataSyncHandler.<BridgeConfiguration>builder()
      .key("bridge-config")
      .nameExtractor($ -> "Bridge Config")
      .convertObject(BridgeConfiguration.class)
      .writer(management::configuration)
      .singletonCollector(management::configuration)
      .currentGetter($ -> management.configuration())
      .build());
    // register the bridge rest handler
    Node.instance().httpServer()
      .registerHandler("/api/v2/player", new V2HttpHandlerBridge("http.v2.bridge"))
      .registerHandler("/api/v2/player/{identifier}", new V2HttpHandlerBridge("http.v2.bridge"))
      .registerHandler("/api/v2/player/{identifier}/exists", HttpHandler.PRIORITY_LOW,
        new V2HttpHandlerBridge("http.v2.bridge"));
  }

  @ModuleTask(event = ModuleLifeCycle.STARTED)
  public void registerCommand() {
    // register the bridge command
    Node.instance().commandProvider().register(new CommandBridge(ServiceRegistry.first(BridgeManagement.class)));
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    var management = ServiceRegistry.first(BridgeManagement.class);
    if (management != null) {
      management.configuration(this.loadConfiguration());
    }
  }

  private @NonNull BridgeConfiguration loadConfiguration() {
    return this.readConfig(BridgeConfiguration.class, BridgeConfiguration::new);
  }
}
