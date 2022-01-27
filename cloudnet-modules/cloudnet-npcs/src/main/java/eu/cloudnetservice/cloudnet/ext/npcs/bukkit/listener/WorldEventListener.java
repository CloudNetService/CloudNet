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
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

public final class WorldEventListener implements Listener {

  private final BukkitNPCManagement management;

  public WorldEventListener(@NotNull BukkitNPCManagement management) {
    this.management = management;
  }

  @EventHandler
  public void handle(@NotNull ChunkUnloadEvent event) {
    this.getNPCsInChunk(event.getChunk())
      .forEach(npc -> this.management.getInfoLineStand(npc, false).getSecond().ifPresent(Entity::remove));
  }

  @EventHandler
  public void handle(@NotNull ChunkLoadEvent event) {
    this.getNPCsInChunk(event.getChunk()).forEach(this.management::updateNPC);
  }

  private @NotNull Collection<CloudNPC> getNPCsInChunk(@NotNull Chunk chunk) {
    return this.management.getCloudNPCS().stream()
      .filter(npc -> {
        int chunkX = NumberConversions.floor(npc.getPosition().getX()) >> 4;
        int chunkZ = NumberConversions.floor(npc.getPosition().getZ()) >> 4;

        return chunk.getX() == chunkX && chunk.getZ() == chunkZ;
      })
      .collect(Collectors.toSet());
  }
}
