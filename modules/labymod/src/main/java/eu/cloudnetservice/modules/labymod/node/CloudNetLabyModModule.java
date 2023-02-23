/*
 * Copyright 2019-2023 CloudNetService team & contributors
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
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.modules.labymod.config.LabyModBanner;
import eu.cloudnetservice.modules.labymod.config.LabyModConfiguration;
import eu.cloudnetservice.modules.labymod.config.LabyModDiscordRPC;
import eu.cloudnetservice.modules.labymod.config.LabyModPermissions;
import eu.cloudnetservice.modules.labymod.config.LabyModServiceDisplay;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.module.listener.PluginIncludeListener;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import lombok.NonNull;

@Singleton
public class CloudNetLabyModModule extends DriverModule {

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.LOADED)
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
            .build()
        ));
      }
    }
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.LOADED)
  public void initManagement(
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull @Named("module") InjectionLayer<?> layer
  ) {
    // construct the management instance
    var management = this.readConfigAndInstantiate(
      layer,
      LabyModConfiguration.class,
      () -> LabyModConfiguration.builder().build(),
      NodeLabyModManagement.class);

    // sync the config of the module into the cluster
    dataSyncRegistry.registerHandler(
      DataSyncHandler.<LabyModConfiguration>builder()
        .key("labymod-config")
        .nameExtractor($ -> "LabyMod Config")
        .convertObject(LabyModConfiguration.class)
        .writer(management::configuration)
        .singletonCollector(management::configuration)
        .currentGetter($ -> management.configuration())
        .build());
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.RELOADING)
  public void handleReload(@NonNull NodeLabyModManagement management) {
    management.configuration(this.loadConfiguration());
  }

  @ModuleTask(order = 16, lifecycle = ModuleLifeCycle.LOADED)
  public void initListeners(
    @NonNull ModuleHelper moduleHelper,
    @NonNull EventManager eventManager,
    @NonNull NodeLabyModManagement management
  ) {
    // register the listeners
    eventManager.registerListener(NodeLabyModListener.class);
    eventManager.registerListener(new PluginIncludeListener(
      "cloudnet-labymod",
      CloudNetLabyModModule.class,
      moduleHelper,
      service -> {
        if (management.configuration().enabled()) {
          return service.serviceId().environment().readProperty(ServiceEnvironmentType.JAVA_PROXY);
        }
        return false;
      }));
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
