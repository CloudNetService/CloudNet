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

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.sync.DataSyncHandler;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.config.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.config.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.rpc.TextComponentObjectSerializer;
import de.dytanic.cloudnet.ext.bridge.rpc.TitleObjectSerializer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;

public final class CloudNetBridgeModule extends DriverModule {

  @ModuleTask(order = 50, event = ModuleLifeCycle.LOADED)
  public void initNetworkHelpers() {
    DefaultObjectMapper.DEFAULT_MAPPER.registerBinding(Title.class, new TitleObjectSerializer(), false);
    DefaultObjectMapper.DEFAULT_MAPPER.registerBinding(TextComponent.class, new TextComponentObjectSerializer(), false);
  }

  @ModuleTask(order = 40, event = ModuleLifeCycle.LOADED)
  public void convertOldConfiguration() {
    Path oldConfigurationPath = this.getModuleWrapper()
      .getModuleProvider()
      .getModuleDirectoryPath()
      .resolve("CloudNet-Bridge")
      .resolve("config.json");
    // check if the old file exists
    if (Files.exists(oldConfigurationPath)) {
      // read the file
      JsonDocument config = JsonDocument.newDocument(oldConfigurationPath).getDocument("config");
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
      )).write(this.getConfigPath());
      // delete the old config
      FileUtils.delete(oldConfigurationPath.getParent());
    }
  }

  @ModuleTask(event = ModuleLifeCycle.LOADED)
  public void initModule() {
    // load the configuration file
    BridgeConfiguration configuration = this.readConfig().toInstanceOf(BridgeConfiguration.class);
    if (Files.notExists(this.getConfigPath())) {
      // create a new configuration
      configuration = new BridgeConfiguration();
      this.writeConfig(JsonDocument.newDocument(configuration));
    }
    // init the bridge management
    BridgeManagement management = new NodeBridgeManagement(
      this,
      configuration,
      this.getEventManager(),
      CloudNet.getInstance().getDataSyncRegistry(),
      this.getRPCFactory());
    management.registerServices(this.getServiceRegistry());
    management.postInit();
    // register the cluster sync handler
    CloudNet.getInstance().getDataSyncRegistry().registerHandler(DataSyncHandler.<BridgeConfiguration>builder()
      .key("bridge-config")
      .nameExtractor($ -> "Bridge Config")
      .convertObject(BridgeConfiguration.class)
      .writer(management::setConfiguration)
      .singletonCollector(management::getConfiguration)
      .currentGetter($ -> management.getConfiguration())
      .build());
  }
}
