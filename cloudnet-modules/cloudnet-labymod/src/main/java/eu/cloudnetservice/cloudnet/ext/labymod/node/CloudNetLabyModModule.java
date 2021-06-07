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

package eu.cloudnetservice.cloudnet.ext.labymod.node;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.config.DiscordJoinMatchConfig;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModBannerConfig;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModPermissionConfig;
import eu.cloudnetservice.cloudnet.ext.labymod.config.ServiceDisplay;
import eu.cloudnetservice.cloudnet.ext.labymod.node.listener.IncludePluginListener;
import eu.cloudnetservice.cloudnet.ext.labymod.node.listener.LabyModCustomChannelMessageListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CloudNetLabyModModule extends NodeCloudNetModule {

  private static final Map<String, Boolean> DEFAULT_LABY_MOD_PERMISSIONS = new HashMap<>();
  private static final LabyModPermissionConfig LABY_MOD_PERMISSION_CONFIG = new LabyModPermissionConfig(false,
    DEFAULT_LABY_MOD_PERMISSIONS);
  private static final LabyModBannerConfig BANNER_CONFIG = new LabyModBannerConfig(false,
    "http://dl.cloudnetservice.eu/data/minecraft/CloudNet-LabyMod-Banner.png");

  static {
    DEFAULT_LABY_MOD_PERMISSIONS.put("IMPROVED_LAVA", false);
    DEFAULT_LABY_MOD_PERMISSIONS.put("CROSSHAIR_SYNC", false);
    DEFAULT_LABY_MOD_PERMISSIONS.put("REFILL_FIX", false);
    DEFAULT_LABY_MOD_PERMISSIONS.put("GUI_ALL", true);
    DEFAULT_LABY_MOD_PERMISSIONS.put("GUI_POTION_EFFECTS", true);
    DEFAULT_LABY_MOD_PERMISSIONS.put("GUI_ARMOR_HUD", true);
    DEFAULT_LABY_MOD_PERMISSIONS.put("GUI_ITEM_HUD", true);
    DEFAULT_LABY_MOD_PERMISSIONS.put("BLOCKBUILD", true);
    DEFAULT_LABY_MOD_PERMISSIONS.put("TAGS", true);
    DEFAULT_LABY_MOD_PERMISSIONS.put("CHAT", true);
    DEFAULT_LABY_MOD_PERMISSIONS.put("ANIMATIONS", true);
    DEFAULT_LABY_MOD_PERMISSIONS.put("SATURATION_BAR", true);
    DEFAULT_LABY_MOD_PERMISSIONS.put("RANGE", false);
    DEFAULT_LABY_MOD_PERMISSIONS.put("SLOWDOWN", false);
  }

  private LabyModConfiguration configuration;

  @ModuleTask(event = ModuleLifeCycle.STARTED)
  public void loadConfig() {
    FileUtils.createDirectoryReported(super.getModuleWrapper().getDataDirectory());

    JsonDocument previousConfig = super.getConfig().clone();
    this.configuration = super.getConfig().get("config", LabyModConfiguration.class, new LabyModConfiguration(
      false,
      new ServiceDisplay(true, ServiceDisplay.DisplayType.SERVICE, "Playing on %display%"),
      new DiscordJoinMatchConfig(true, new ArrayList<>()),
      new ServiceDisplay(true, ServiceDisplay.DisplayType.TASK, "§bCloud§fNet §8➢ §e%display%"),
      "mc.example.com",
      true,
      new ArrayList<>(),
      LABY_MOD_PERMISSION_CONFIG,
      BANNER_CONFIG
    ));

    if (this.configuration.getPermissionConfig() == null) {
      this.configuration.setPermissionConfig(LABY_MOD_PERMISSION_CONFIG);
    }

    if (this.configuration.getBannerConfig() == null) {
      this.configuration.setBannerConfig(BANNER_CONFIG);
    }

    super.getConfig().append("config", this.configuration);

    if (!previousConfig.equals(super.getConfig())) {
      super.saveConfig();
    }
  }

  @ModuleTask(event = ModuleLifeCycle.STARTED)
  public void initListeners() {
    super.registerListeners(new LabyModCustomChannelMessageListener(this), new IncludePluginListener(this));
  }

  public LabyModConfiguration getConfiguration() {
    return this.configuration;
  }

  public boolean isSupportedEnvironment(ServiceEnvironmentType environment) {
    return LabyModConstants.SUPPORTED_ENVIRONMENTS.contains(environment);
  }

}
