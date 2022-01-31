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

package eu.cloudnetservice.modules.labymod.node;

import com.google.gson.reflect.TypeToken;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.cloudnet.driver.module.ModuleTask;
import eu.cloudnetservice.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.modules.labymod.config.LabyModBanner;
import eu.cloudnetservice.modules.labymod.config.LabyModConfiguration;
import eu.cloudnetservice.modules.labymod.config.LabyModDiscordRPC;
import eu.cloudnetservice.modules.labymod.config.LabyModPermissions;
import eu.cloudnetservice.modules.labymod.config.LabyModServiceDisplay;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import lombok.NonNull;

public class CloudNetLabyModModule extends DriverModule {

  private NodeLabyModManagement labyModManagement;

  @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
  public void convertConfig() {
    if (Files.exists(this.configPath())) {
      var config = this.readConfig().getDocument("config");
      // there is a config, run the conversion
      if (!config.empty()) {
        // rewrite the config with all settings from the old config, but in the new format
        this.writeConfig(JsonDocument.newDocument(
          LabyModConfiguration.builder()
            .enabled(config.getBoolean("enabled"))
            .discordRPC(this.convertDisplayEntry(config.getDocument("discordRPC")))
            .gameModeSwitch(this.convertDisplayEntry(config.getDocument("gameModeSwitchMessages")))
            .joinMatch(config.getDocument("discordJoinMatch").toInstanceOf(LabyModDiscordRPC.class))
            .spectateMatch(LabyModDiscordRPC.builder()
              .enabled(config.getBoolean("discordSpectateEnabled"))
              .excludedGroups(config.get("excludedSpectateGroups",
                TypeToken.getParameterized(Collection.class, String.class).getType()))
              .build())
            .loginDomain(config.getString("loginDomain"))
            .banner(config.get("bannerConfig", LabyModBanner.class))
            .permissions(LabyModPermissions.builder()
              .enabled(config.getDocument("permissionConfig").getBoolean("enabled"))
              .permissions(config.getDocument("permissionConfig")
                .get("labyModPermissions",
                  TypeToken.getParameterized(Map.class, String.class, Boolean.class).getType()))
              .build())
        ));
      }
    }
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.LOADED)
  public void initManagement() {
    this.labyModManagement = new NodeLabyModManagement(this, this.loadConfiguration(), this.rpcFactory());
    // sync the config of the module into the cluster
    CloudNet.instance().dataSyncRegistry().registerHandler(
      DataSyncHandler.<LabyModConfiguration>builder()
        .key("labymod-config")
        .nameExtractor($ -> "LabyMod Config")
        .convertObject(LabyModConfiguration.class)
        .writer(this.labyModManagement::configuration)
        .singletonCollector(this.labyModManagement::configuration)
        .currentGetter($ -> this.labyModManagement.configuration())
        .build());
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    this.labyModManagement.configuration(this.loadConfiguration());
  }

  @ModuleTask(order = 64, event = ModuleLifeCycle.LOADED)
  public void initListeners() {
    // register the listeners
    this.registerListener(new NodeLabyModListener(this.labyModManagement));
  }

  private @NonNull LabyModServiceDisplay convertDisplayEntry(@NonNull JsonDocument entry) {
    var enabled = entry.getBoolean("enabled");
    var format = entry.getString("format");
    var displayType = entry.getString("displayType");
    // convert the old display type
    if (displayType.equals("TASK")) {
      return new LabyModServiceDisplay(enabled, format.replace("%display%", "%task%"));
    } else {
      return new LabyModServiceDisplay(enabled, format.replace("%display%", "%name%"));
    }
  }

  private @NonNull LabyModConfiguration loadConfiguration() {
    return this.readConfig(LabyModConfiguration.class, () -> LabyModConfiguration.builder().build());
  }
}
