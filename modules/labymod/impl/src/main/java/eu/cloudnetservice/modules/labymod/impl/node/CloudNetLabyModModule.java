/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.labymod.impl.node;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.labymod.config.LabyModBanner;
import eu.cloudnetservice.modules.labymod.config.LabyModConfiguration;
import eu.cloudnetservice.modules.labymod.config.LabyModPermissions;
import eu.cloudnetservice.modules.labymod.config.LabyModServiceDisplay;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.impl.module.listener.PluginIncludeListener;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.nio.file.Files;
import java.util.Map;
import lombok.NonNull;

@Singleton
public class CloudNetLabyModModule extends DriverModule {

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.LOADED)
  public void convertConfig() {
    if (Files.exists(this.configPath())) {
      // there is a config, run the conversion
      var config = this.readConfig(DocumentFactory.json()).readDocument("config");
      if (!config.empty()) {
        // rewrite the config with all settings from the old config, but in the new format
        this.writeConfig(Document.newJsonDocument().appendTree(
          LabyModConfiguration.builder()
            .enabled(config.getBoolean("enabled"))
            .discordRPC(this.convertDisplayEntry(config.readDocument("discordRPC")))
            .banner(config.readObject("bannerConfig", LabyModBanner.class))
            .permissions(LabyModPermissions.builder()
              .enabled(config.readDocument("permissionConfig").getBoolean("enabled"))
              .permissions(config.readDocument("permissionConfig")
                .readObject(
                  "labyModPermissions",
                  TypeFactory.parameterizedClass(Map.class, String.class, Boolean.class)))
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
      NodeLabyModManagement.class,
      DocumentFactory.json());

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

  private @NonNull LabyModServiceDisplay convertDisplayEntry(@NonNull Document entry) {
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
    return this.readConfig(
      LabyModConfiguration.class,
      () -> LabyModConfiguration.builder().build(),
      DocumentFactory.json());
  }
}
