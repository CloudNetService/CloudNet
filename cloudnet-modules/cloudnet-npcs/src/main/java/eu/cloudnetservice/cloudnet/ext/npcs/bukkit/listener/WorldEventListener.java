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

package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener;

import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.BukkitNPCManagement;
import java.util.Collection;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class WorldEventListener implements Listener {

  private final Plugin plugin;
  private final BukkitNPCManagement management;

  public WorldEventListener(@NotNull Plugin plugin, @NotNull BukkitNPCManagement management) {
    this.plugin = plugin;
    this.management = management;
  }

  @EventHandler
  public void handle(@NotNull WorldSaveEvent event) {
    Collection<CloudNPC> npcs = this.management.getCloudNPCS().stream()
      .filter(npc -> npc.getPosition().getWorld().equals(event.getWorld().getName()))
      .collect(Collectors.toSet());
    // remove all info line stands
    npcs.forEach(npc -> this.management.getInfoLineStand(npc, false).ifPresent(Entity::remove));
    // respawn the armor stands after two seconds and hope that the world save is done
    Bukkit.getScheduler().runTaskLater(this.plugin, () -> npcs.forEach(this.management::updateNPC), 2 * 20);
  }
}
