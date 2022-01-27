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

package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.npcs.AbstractNPCManagement;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.command.CloudNPCCommand;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.labymod.LabyModEmotePlayer;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener.NPCInventoryListener;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener.WorldEventListener;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitCloudNetNPCPlugin extends JavaPlugin {

  private BukkitNPCManagement npcManagement;

  @Override
  public void onEnable() {
    this.npcManagement = new BukkitNPCManagement(this);
    CloudNetDriver.getInstance().getServicesRegistry()
      .registerService(AbstractNPCManagement.class, "BukkitNPCManagement", this.npcManagement);

    this.registerListeners();
  }

  private void registerListeners() {
    PluginCommand pluginCommand = this.getCommand("cloudnpc");

    if (pluginCommand != null) {
      pluginCommand.setExecutor(new CloudNPCCommand(this.npcManagement));
      pluginCommand.setPermission("cloudnet.command.cloudnpc");
      pluginCommand.setUsage(
        "/cloudnpc create <targetGroup> <displayName> <skinUUID> <itemInHand> <shouldLookAtPlayer> <shouldImitatePlayer>");
      pluginCommand.setDescription("Adds or removes server selector NPCs");
    }

    CloudNetDriver.getInstance().getEventManager().registerListener(this.npcManagement);
    Bukkit.getPluginManager().registerEvents(new NPCInventoryListener(this.npcManagement), this);
    Bukkit.getPluginManager().registerEvents(new WorldEventListener(this.npcManagement), this);
    Bukkit.getPluginManager().registerEvents(new LabyModEmotePlayer(this, this.npcManagement), this);

    NPCConfigurationEntry ownNPCConfigurationEntry = this.npcManagement.getOwnNPCConfigurationEntry();

    if (ownNPCConfigurationEntry != null
      && ownNPCConfigurationEntry.getKnockbackDistance() > 0
      && ownNPCConfigurationEntry.getKnockbackStrength() > 0) {
      Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new BukkitNPCKnockbackRunnable(this.npcManagement), 20, 5);
    }
  }

  @Override
  public void onDisable() {
    if (this.npcManagement != null) {
      CloudNetDriver.getInstance().getEventManager().unregisterListener(this.npcManagement);

      this.npcManagement.shutdown();
      CloudNetDriver.getInstance().getServicesRegistry()
        .unregisterService(AbstractNPCManagement.class, this.npcManagement);
    }
  }
}
