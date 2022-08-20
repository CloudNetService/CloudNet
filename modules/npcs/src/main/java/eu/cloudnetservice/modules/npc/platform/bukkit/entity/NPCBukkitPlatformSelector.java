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

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.flag.NpcFlag;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.profile.ProfileProperty;
import com.github.juliarn.npclib.bukkit.util.BukkitPlatformUtil;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import eu.cloudnetservice.modules.npc.platform.bukkit.BukkitPlatformNPCManagement;
import eu.cloudnetservice.modules.npc.platform.util.UserNameUtil;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class NPCBukkitPlatformSelector extends BukkitPlatformSelectorEntity {

  public static final NpcFlag<PlatformSelectorEntity<Location, Player, ItemStack, Inventory>> SELECTOR_ENTITY = NpcFlag.flag(
    "cloudnet_selector_entity",
    null);

  protected final Platform<World, Player, ItemStack, Plugin> platform;
  protected volatile Npc<World, Player, ItemStack, Plugin> handleNpc;

  public NPCBukkitPlatformSelector(
    @NonNull BukkitPlatformNPCManagement npcManagement,
    @NonNull Plugin plugin,
    @NonNull NPC npc,
    @NonNull Platform<World, Player, ItemStack, Plugin> platform
  ) {
    super(npcManagement, plugin, npc);
    this.platform = platform;
  }

  @Override
  public int entityId() {
    return this.handleNpc == null ? -1 : this.handleNpc.entityId();
  }

  @Override
  public @NonNull String scoreboardRepresentation() {
    return this.handleNpc.profile().name();
  }

  @Override
  public boolean spawned() {
    return this.handleNpc != null;
  }

  @Override
  protected void spawn0() {
    this.handleNpc = this.platform.newNpcBuilder()
      .flag(SELECTOR_ENTITY, this)
      .flag(Npc.SNEAK_WHEN_PLAYER_SNEAKS, this.npc.imitatePlayer())
      .flag(Npc.HIT_WHEN_PLAYER_HITS, this.npc.imitatePlayer())
      .flag(Npc.LOOK_AT_PLAYER, this.npc.lookAtPlayer())
      .flag(Npc.DISPLAY_NAME, this.npc.displayName())
      .npcSettings(builder -> builder.profileResolver((player, spawnedNpc) -> {
        if (this.npc.usePlayerSkin()) {
          return this.platform.profileResolver().resolveProfile(Profile.unresolved(player.getUniqueId()));
        } else {
          return CompletableFuture.completedFuture(spawnedNpc.profile());
        }
      }))
      .profile(Profile.resolved(
        UserNameUtil.convertStringToValidName(this.npc.displayName()),
        this.uniqueId,
        this.npc.profileProperties().stream()
          .map(prop -> ProfileProperty.property(prop.name(), prop.value(), prop.signature()))
          .collect(Collectors.toSet())))
      .position(BukkitPlatformUtil.positionFromBukkit(this.npcLocation))
      .buildAndTrack();
  }

  @Override
  protected void remove0() {
    this.handleNpc.unlink();
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

  public @NonNull Npc<World, Player, ItemStack, Plugin> handleNPC() {
    return this.handleNpc;
  }
}
