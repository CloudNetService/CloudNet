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

import static de.dytanic.cloudnet.ext.bridge.BridgeManagement.BRIDGE_PLAYER_DB_NAME;

import com.google.common.collect.Iterables;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.sync.DataSyncHandler;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.config.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.config.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.node.command.CommandBridge;
import de.dytanic.cloudnet.ext.bridge.rpc.ComponentObjectSerializer;
import de.dytanic.cloudnet.ext.bridge.rpc.TitleObjectSerializer;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public final class CloudNetBridgeModule extends DriverModule {

  private static final Logger LOGGER = LogManager.logger(CloudNetBridgeModule.class);

  @ModuleTask(order = 50, event = ModuleLifeCycle.LOADED)
  public void initNetworkHelpers() {
    DefaultObjectMapper.DEFAULT_MAPPER.registerBinding(Title.class, new TitleObjectSerializer(), false);
    DefaultObjectMapper.DEFAULT_MAPPER.registerBinding(Component.class, new ComponentObjectSerializer(), false);
  }

  @ModuleTask(order = 40, event = ModuleLifeCycle.LOADED)
  public void convertOldConfiguration() {
    var oldConfigurationPath = this.moduleWrapper()
      .moduleProvider()
      .moduleDirectoryPath()
      .resolve("CloudNet-Bridge")
      .resolve("config.json");
    // check if the old file exists
    if (Files.exists(oldConfigurationPath)) {
      // read the file
      var config = JsonDocument.newDocument(oldConfigurationPath).getDocument("config");
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
      // delete the old config
      FileUtils.delete(oldConfigurationPath.getParent());
    }
  }

  @ModuleTask(event = ModuleLifeCycle.STARTED)
  public void convertOldDatabaseEntries() {
    var playerDb = CloudNet.getInstance().databaseProvider().database(BRIDGE_PLAYER_DB_NAME);
    // read the first player from the database - if the first player is valid we don't need to take a look at the other
    // players in the database as they were already converted
    var first = playerDb.readChunk(101, 1);
    if (first != null && !first.isEmpty()) {
      var document = Iterables.getOnlyElement(first.values());
      // validate the offline player
      var serviceId = document
        .getDocument("lastNetworkPlayerProxyInfo")
        .getDocument("networkService")
        .getDocument("serviceId");
      // check if the environment name is set
      if (serviceId.getString("environmentName") == null) {
        LOGGER.warning("Converting the offline player database, this may take a few seconds...");

        var convertedPlayers = 0;
        // invalid player data - convert the database
        Map<String, JsonDocument> chunkData;
        while ((chunkData = playerDb.readChunk(convertedPlayers, 100)) != null) {
          for (var entry : chunkData.entrySet()) {
            // get all the required path
            var lastProxyInfo = entry.getValue().getDocument("lastNetworkPlayerProxyInfo");
            var networkService = lastProxyInfo.getDocument("networkService");
            serviceId = networkService.getDocument("serviceId");
            // rewrite the name of the environment
            var environment = serviceId.getString("environment", "");
            serviceId.append("environmentName", environment);
            // try to set the new environment
            var env = CloudNet.getInstance().getServiceVersionProvider()
              .getEnvironmentType(environment)
              .orElse(null);
            serviceId.append("environment", env);
            // rewrite all paths of the document
            networkService.append("serviceId", serviceId);
            lastProxyInfo.append("networkService", networkService);
            entry.getValue().append("name", lastProxyInfo.get("name"));
            entry.getValue().append("lastNetworkPlayerProxyInfo", lastProxyInfo);
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
    // load the configuration file
    var configuration = this.readConfig().toInstanceOf(BridgeConfiguration.class);
    if (Files.notExists(this.configPath())) {
      // create a new configuration
      configuration = new BridgeConfiguration();
      this.writeConfig(JsonDocument.newDocument(configuration));
    }
    // init the bridge management
    BridgeManagement management = new NodeBridgeManagement(
      this,
      configuration,
      this.eventManager(),
      CloudNet.getInstance().getDataSyncRegistry(),
      this.rpcFactory());
    management.registerServices(this.serviceRegistry());
    management.postInit();
    // register the cluster sync handler
    CloudNet.getInstance().getDataSyncRegistry().registerHandler(DataSyncHandler.<BridgeConfiguration>builder()
      .key("bridge-config")
      .nameExtractor($ -> "Bridge Config")
      .convertObject(BridgeConfiguration.class)
      .writer(management::configuration)
      .singletonCollector(management::configuration)
      .currentGetter($ -> management.configuration())
      .build());
    // register the bridge command
    CloudNet.getInstance().getCommandProvider().register(new CommandBridge(management));
  }
}
