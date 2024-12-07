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

package eu.cloudnetservice.modules.npc.impl.platform.bukkit.listener;

import eu.cloudnetservice.modules.npc.impl.platform.bukkit.BukkitPlatformNPCManagement;
import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.NumberConversions;

@Singleton
public final class BukkitWorldListener implements Listener {

  private final Plugin plugin;
  private final BukkitScheduler scheduler;
  private final BukkitPlatformNPCManagement management;

  @Inject
  public BukkitWorldListener(
    @NonNull Plugin plugin,
    @NonNull BukkitScheduler scheduler,
    @NonNull BukkitPlatformNPCManagement management
  ) {
    this.plugin = plugin;
    this.scheduler = scheduler;
    this.management = management;
  }

  @EventHandler
  public void handle(@NonNull ChunkLoadEvent event) {
    this.management.trackedEntities().values()
      .stream()
      .filter(npc -> !npc.spawned())
      .filter(npc -> npc.location().getWorld() != null) // cannot use canSpawn as the chunk is not marked as loaded yet
      .filter(npc -> npc.npc().location().world().equals(event.getWorld().getName()))
      .filter(npc -> {
        var chunkX = NumberConversions.floor(npc.location().getX()) >> 4;
        var chunkZ = NumberConversions.floor(npc.location().getZ()) >> 4;
        // validate that the entity is in the chunk being loaded - Location#getChunk causes a load of the chunk
        return event.getChunk().getX() == chunkX && event.getChunk().getZ() == chunkZ;
      })
      .forEach(PlatformSelectorEntity::spawn);
  }

  @EventHandler
  public void handle(@NonNull ChunkUnloadEvent event) {
    this.management.trackedEntities().values()
      .stream()
      .filter(PlatformSelectorEntity::spawned)
      .filter(npc -> npc.location().getWorld().getUID().equals(event.getWorld().getUID()))
      .filter(npc -> {
        var chunkX = NumberConversions.floor(npc.location().getX()) >> 4;
        var chunkZ = NumberConversions.floor(npc.location().getZ()) >> 4;
        // validate that the entity is in the chunk being unloaded - Location#getChunk causes a load of the chunk
        return event.getChunk().getX() == chunkX && event.getChunk().getZ() == chunkZ;
      })
      .forEach(PlatformSelectorEntity::remove);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handleWorldSave(@NonNull WorldSaveEvent event) {
    var entities = this.management.trackedEntities().values().stream()
      .filter(PlatformSelectorEntity::spawned)
      .filter(npc -> npc.location().getWorld().getUID().equals(event.getWorld().getUID()))
      .toList();
    // remove all mobs
    entities.forEach(PlatformSelectorEntity::remove);
    // re-spawn all entities after 2 seconds - just hope the world save is done
    this.scheduler.runTaskLater(this.plugin, () -> entities.forEach(PlatformSelectorEntity::spawn), 2 * 20);
  }
}
