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

package eu.cloudnetservice.modules.npc.platform.bukkit.entity;

import com.github.juliarn.npc.NPCPool;
import com.github.juliarn.npc.modifier.AnimationModifier.EntityAnimation;
import com.github.juliarn.npc.modifier.MetadataModifier.EntityMetadata;
import com.github.juliarn.npc.modifier.NPCModifier;
import com.github.juliarn.npc.profile.Profile;
import com.github.juliarn.npc.profile.Profile.Property;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.platform.bukkit.BukkitPlatformNPCManagement;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class NPCBukkitPlatformSelector extends BukkitPlatformSelectorEntity {

  // See: https://wiki.vg/Entity_metadata#Entity
  private static final byte GLOWING_FLAGS = 1 << 6;
  private static final byte ELYTRA_FLYING_FLAGS = (byte) (1 << 7);
  private static final byte FLYING_AND_GLOWING = (byte) (GLOWING_FLAGS | ELYTRA_FLYING_FLAGS);

  protected final NPCPool npcPool;
  protected volatile com.github.juliarn.npc.NPC handleNpc;

  public NPCBukkitPlatformSelector(
    @NonNull BukkitPlatformNPCManagement npcManagement,
    @NonNull Plugin plugin,
    @NonNull NPC npc,
    @NonNull NPCPool pool
  ) {
    super(npcManagement, plugin, npc);
    this.npcPool = pool;
  }

  @Override
  public int entityId() {
    return this.handleNpc == null ? -1 : this.handleNpc.getEntityId();
  }

  @Override
  public @NonNull String scoreboardRepresentation() {
    return this.handleNpc.getProfile().getName();
  }

  @Override
  public boolean removeWhenWorldSaving() {
    return false;
  }

  @Override
  public boolean spawned() {
    return this.handleNpc != null;
  }

  @Override
  protected void spawn0() {
    this.handleNpc = com.github.juliarn.npc.NPC.builder()
      .imitatePlayer(this.npc.imitatePlayer())
      .lookAtPlayer(this.npc.lookAtPlayer())
      .usePlayerProfiles(this.npc.usePlayerSkin())
      .profile(new Profile(
        new UUID(ThreadLocalRandom.current().nextLong(), 0),
        this.npc.displayName(),
        this.npc.profileProperties().stream()
          .map(prop -> new Property(prop.name(), prop.value(), prop.signature()))
          .collect(Collectors.toSet())
      ))
      .location(this.npcLocation)
      .spawnCustomizer((spawnedNpc, player) -> {
        // just because the client is stupid sometimes
        spawnedNpc.rotation().queueRotate(this.npcLocation.getYaw(), this.npcLocation.getPitch()).send(player);
        spawnedNpc.animation().queue(EntityAnimation.SWING_MAIN_ARM).send(player);
        var metadataModifier = spawnedNpc.metadata()
          .queue(EntityMetadata.SKIN_LAYERS, true)
          .queue(EntityMetadata.SNEAKING, false);
        // apply glowing effect if possible
        if (NPCModifier.MINECRAFT_VERSION >= 9) {
          if (this.npc.glowing() && this.npc.flyingWithElytra()) {
            metadataModifier.queue(0, FLYING_AND_GLOWING, Byte.class);
          } else if (this.npc.glowing()) {
            metadataModifier.queue(0, GLOWING_FLAGS, Byte.class);
          } else if (this.npc.flyingWithElytra()) {
            metadataModifier.queue(0, ELYTRA_FLYING_FLAGS, Byte.class);
          }
        }
        metadataModifier.send(player);
        // set the items
        var modifier = spawnedNpc.equipment();
        for (var entry : this.npc.items().entrySet()) {
          if (entry.getKey() >= 0 && entry.getKey() <= 5) {
            var material = Material.matchMaterial(entry.getValue());
            if (material != null) {
              modifier.queue(entry.getKey(), new ItemStack(material));
            }
          }
        }
        modifier.send(player);
      }).build(this.npcPool);
  }

  @Override
  protected void remove0() {
    this.npcPool.removeNPC(this.handleNpc.getEntityId());
    this.handleNpc = null;
  }

  @Override
  protected void addGlowingEffect() {
    // no-op - we're doing this while spawning to the player
  }

  @Override
  protected double heightAddition(int lineNumber) {
    return 1.09 + super.heightAddition(lineNumber);
  }

  public @NonNull com.github.juliarn.npc.NPC handleNPC() {
    return this.handleNpc;
  }
}
