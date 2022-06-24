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

package eu.cloudnetservice.modules.npc.platform.bukkit;

import com.github.juliarn.npc.NPCPool;
import com.github.juliarn.npc.modifier.LabyModModifier;
import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.platform.PlatformNPCManagement;
import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import eu.cloudnetservice.modules.npc.platform.bukkit.entity.EntityBukkitPlatformSelectorEntity;
import eu.cloudnetservice.modules.npc.platform.bukkit.entity.NPCBukkitPlatformSelector;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.NumberConversions;

public class BukkitPlatformNPCManagement extends PlatformNPCManagement<Location, Player, ItemStack, Inventory> {

  protected final Plugin plugin;
  protected final NPCPool npcPool;
  protected final Scoreboard scoreboard;
  protected final BukkitTask knockBackTask;

  protected volatile BukkitTask npcEmoteTask;

  public BukkitPlatformNPCManagement(@NonNull Plugin plugin) {
    this.plugin = plugin;
    this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

    // npc pool init
    var entry = this.applicableNPCConfigurationEntry();
    if (entry != null) {
      this.npcPool = NPCPool.builder(plugin)
        .actionDistance(entry.npcPoolOptions().actionDistance())
        .spawnDistance(entry.npcPoolOptions().spawnDistance())
        .tabListRemoveTicks(entry.npcPoolOptions().tabListRemoveTicks())
        .build();
    } else {
      this.npcPool = NPCPool.builder(plugin).build();
    }

    // start the emote player
    this.startEmoteTask(false);
    // start the knock back task
    this.knockBackTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      var configEntry = this.applicableNPCConfigurationEntry();
      if (configEntry != null) {
        // check if knock back is enabled
        var distance = configEntry.knockbackDistance();
        var strength = configEntry.knockbackStrength();
        if (distance > 0 && strength > 0) {
          // select the knockback emote id now (sometimes we need to play them sync for all npcs)
          var labyModEmotes = configEntry.emoteConfiguration().onKnockbackEmoteIds();
          var emoteId = this.randomEmoteId(configEntry.emoteConfiguration(), labyModEmotes);
          //
          for (var value : this.trackedEntities.values()) {
            if (value.spawned()) {
              // select all nearby entities of each spawned mob
              var nearbyEntities = value.location().getWorld().getNearbyEntities(
                value.location(),
                distance,
                distance,
                distance);
              // loop over all entities and knock them back
              if (!nearbyEntities.isEmpty()) {
                for (var entity : nearbyEntities) {
                  // check if the entity is a player
                  if (entity instanceof Player player && !entity.hasPermission("cloudnet.npcs.knockback.bypass")) {
                    // apply the strength to the curren vector
                    var vector = player.getLocation().toVector().subtract(value.location().toVector())
                      .normalize()
                      .multiply(strength)
                      .setY(0.2);
                    if (NumberConversions.isFinite(vector.getX()) && NumberConversions.isFinite(vector.getZ())) {
                      // apply the velocity
                      player.setVelocity(vector);
                      // check if we should send a labymod emote
                      if (value instanceof NPCBukkitPlatformSelector npcSelector) {
                        if (emoteId == -1) {
                          var emote = labyModEmotes[ThreadLocalRandom.current().nextInt(0, labyModEmotes.length)];
                          npcSelector.handleNPC()
                            .labymod()
                            .queue(LabyModModifier.LabyModAction.EMOTE, emote)
                            .send(player);
                        } else {
                          // use the selected emote
                          npcSelector.handleNPC()
                            .labymod()
                            .queue(LabyModModifier.LabyModAction.EMOTE, emoteId)
                            .send(player);
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }, 20, 5);
  }

  @NonNull
  @Override
  protected PlatformSelectorEntity<Location, Player, ItemStack, Inventory> createSelectorEntity(@NonNull NPC base) {
    return base.npcType() == NPC.NPCType.ENTITY
      ? new EntityBukkitPlatformSelectorEntity(this, this.plugin, base)
      : new NPCBukkitPlatformSelector(this, this.plugin, base, this.npcPool);
  }

  @Override
  public @NonNull WorldPosition toWorldPosition(@NonNull Location location, @NonNull String group) {
    Preconditions.checkNotNull(location.getWorld(), "world unloaded");
    return new WorldPosition(
      location.getX(),
      location.getY(),
      location.getZ(),
      location.getYaw(),
      location.getPitch(),
      location.getWorld().getName(),
      group);
  }

  @Override
  public @NonNull Location toPlatformLocation(@NonNull WorldPosition position) {
    var world = Bukkit.getWorld(position.world());
    return new Location(
      world,
      position.x(),
      position.y(),
      position.z(),
      (float) position.yaw(),
      (float) position.pitch());
  }

  @Override
  protected boolean shouldTrack(@NonNull ServiceInfoSnapshot service) {
    return service.lifeCycle() == ServiceLifeCycle.RUNNING
      && ServiceEnvironmentType.JAVA_SERVER.get(service.serviceId().environment().properties());
  }

  @Override
  public void handleInternalNPCConfigUpdate(@NonNull NPCConfiguration configuration) {
    super.handleInternalNPCConfigUpdate(configuration);
    // re-schedule the emote task if it's not yet running
    this.startEmoteTask(false);
  }

  public @NonNull Scoreboard scoreboard() {
    return this.scoreboard;
  }

  public @NonNull NPCPool npcPool() {
    return this.npcPool;
  }

  protected void startEmoteTask(boolean force) {
    // only start the task if not yet running
    if (this.npcEmoteTask == null || force) {
      var ent = this.applicableNPCConfigurationEntry();
      if (ent != null && ent.emoteConfiguration().minEmoteDelayTicks() > 0) {
        // get the delay for the next npc emote play
        long delay;
        if (ent.emoteConfiguration().maxEmoteDelayTicks() > ent.emoteConfiguration().minEmoteDelayTicks()) {
          delay = ThreadLocalRandom.current().nextLong(
            ent.emoteConfiguration().minEmoteDelayTicks(),
            ent.emoteConfiguration().maxEmoteDelayTicks());
        } else {
          delay = ent.emoteConfiguration().minEmoteDelayTicks();
        }
        // run the task
        this.npcEmoteTask = Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
          // select an emote to play
          var emotes = ent.emoteConfiguration().emoteIds();
          var emoteId = this.randomEmoteId(ent.emoteConfiguration(), emotes);
          // check if we can select an emote
          if (emoteId >= -1) {
            // play the emote on each npc
            for (var npc : this.npcPool.getNPCs()) {
              if (emoteId == -1) {
                npc.labymod()
                  .queue(
                    LabyModModifier.LabyModAction.EMOTE,
                    emotes[ThreadLocalRandom.current().nextInt(0, emotes.length)])
                  .send();
              } else {
                npc.labymod().queue(LabyModModifier.LabyModAction.EMOTE, emoteId).send();
              }
            }
          }
          // re-schedule
          this.startEmoteTask(true);
        }, delay);
      } else {
        this.npcEmoteTask = null;
      }
    }
  }
}
