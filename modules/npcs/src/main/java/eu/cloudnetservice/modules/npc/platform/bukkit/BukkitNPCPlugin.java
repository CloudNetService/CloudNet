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

package eu.cloudnetservice.modules.npc.platform.bukkit;

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Command;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.npc.platform.bukkit.command.NPCCommand;
import eu.cloudnetservice.modules.npc.platform.bukkit.listener.BukkitEntityProtectionListener;
import eu.cloudnetservice.modules.npc.platform.bukkit.listener.BukkitFunctionalityListener;
import eu.cloudnetservice.modules.npc.platform.bukkit.listener.BukkitWorldListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@PlatformPlugin(
  platform = "bukkit",
  name = "CloudNet-NPCs",
  authors = "CloudNetService",
  version = "@version@",
  homepage = "https://cloudnetservice.eu",
  description = "CloudNet extension which adds NPCs for server selection",
  dependencies = {
    @Dependency(name = "CloudNet-Bridge"),
    @Dependency(name = "ProtocolLib", optional = true)
  },
  commands = @Command(
    name = "cn",
    aliases = "cloudnpc",
    permission = "cloudnet.command.cloudnpc",
    description = "Root command to manage the CloudNet NPC system"
  )
)
public final class BukkitNPCPlugin implements PlatformEntrypoint {

  private final ModuleHelper moduleHelper;
  private final BukkitPlatformNPCManagement npcManagement;

  @Inject
  public BukkitNPCPlugin(@NonNull ModuleHelper moduleHelper, @NonNull BukkitPlatformNPCManagement npcManagement) {
    this.moduleHelper = moduleHelper;
    this.npcManagement = npcManagement;
  }

  @Inject
  private void registerNPCManagement(@NonNull ServiceRegistry serviceRegistry) {
    this.npcManagement.registerToServiceRegistry(serviceRegistry);
    this.npcManagement.initialize();
  }

  @Inject
  private void registerPlatformListener(
    @NonNull Plugin plugin,
    @NonNull PluginManager pluginManager,
    @NonNull BukkitWorldListener worldListener,
    @NonNull BukkitFunctionalityListener functionalityListener,
    @NonNull BukkitEntityProtectionListener entityProtectionListener
  ) {
    pluginManager.registerEvents(worldListener, plugin);
    pluginManager.registerEvents(functionalityListener, plugin);
    pluginManager.registerEvents(entityProtectionListener, plugin);
  }

  @Inject
  private void registerCommand(@NonNull JavaPlugin plugin, @NonNull NPCCommand npcCommand) {
    // only register the command if the command is present
    var command = plugin.getCommand("cn");
    if (command != null) {
      command.setExecutor(npcCommand);
      command.setTabCompleter(npcCommand);
    }
  }

  @Override
  public void onDisable() {
    this.npcManagement.removeSpawnedEntities();
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
