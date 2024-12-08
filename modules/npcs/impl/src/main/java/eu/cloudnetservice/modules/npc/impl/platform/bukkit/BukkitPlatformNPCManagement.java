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

package eu.cloudnetservice.modules.npc.impl.platform.bukkit;

import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.protocol.PlatformPacketAdapter;
import com.github.juliarn.npclib.bukkit.BukkitPlatform;
import com.github.juliarn.npclib.bukkit.BukkitWorldAccessor;
import com.github.juliarn.npclib.bukkit.protocol.BukkitProtocolAdapter;
import com.github.juliarn.npclib.ext.labymod.LabyModExtension;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.util.PEVersion;
import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.NPCManagement;
import eu.cloudnetservice.modules.npc.configuration.NPCConfiguration;
import eu.cloudnetservice.modules.npc.impl.AbstractNPCManagement;
import eu.cloudnetservice.modules.npc.impl.InternalNPCManagement;
import eu.cloudnetservice.modules.npc.impl.platform.PlatformNPCManagement;
import eu.cloudnetservice.modules.npc.impl.platform.bukkit.entity.EntityBukkitPlatformSelectorEntity;
import eu.cloudnetservice.modules.npc.impl.platform.bukkit.entity.NPCBukkitPlatformSelector;
import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.NumberConversions;

@Singleton
@ProvidesFor(platform = "bukkit", types = {
  PlatformNPCManagement.class,
  AbstractNPCManagement.class,
  InternalNPCManagement.class,
  NPCManagement.class
})
public class BukkitPlatformNPCManagement extends
  PlatformNPCManagement<Location, Player, ItemStack, Inventory, Scoreboard> {

  protected final Plugin plugin;
  protected final Server server;
  protected final BukkitScheduler scheduler;
  protected final PlayerManager playerManager;

  protected final Platform<World, Player, ItemStack, Plugin> npcPlatform;
  protected final BukkitTask knockBackTask;

  protected volatile BukkitTask npcEmoteTask;

  @Inject
  public BukkitPlatformNPCManagement(
    @NonNull Plugin plugin,
    @NonNull Server server,
    @NonNull BukkitScheduler scheduler,
    @NonNull EventManager eventManager,
    @NonNull ComponentInfo componentInfo,
    @NonNull @Service PlayerManager playerManager,
    @NonNull CloudServiceProvider cloudServiceProvider,
    @NonNull WrapperConfiguration wrapperConfiguration
  ) {
    super(eventManager, componentInfo, cloudServiceProvider, wrapperConfiguration);

    this.plugin = plugin;
    this.server = server;
    this.scheduler = scheduler;
    this.playerManager = playerManager;

    // npc pool init
    var entry = this.applicableNPCConfigurationEntry();
    if (entry != null) {
      this.npcPlatform = BukkitPlatform.bukkitNpcPlatformBuilder()
        .extension(plugin)
        .debug(true)
        .actionController(builder -> builder
          .flag(NpcActionController.SPAWN_DISTANCE, entry.npcPoolOptions().spawnDistance())
          .flag(NpcActionController.IMITATE_DISTANCE, entry.npcPoolOptions().actionDistance())
          .flag(NpcActionController.TAB_REMOVAL_TICKS, entry.npcPoolOptions().tabListRemoveTicks()))
        .worldAccessor(BukkitWorldAccessor.nameBasedAccessor())
        .packetFactory(this.resolvePacketAdapter())
        .build();
    } else {
      this.npcPlatform = BukkitPlatform.bukkitNpcPlatformBuilder()
        .extension(plugin)
        .worldAccessor(BukkitWorldAccessor.nameBasedAccessor())
        .packetFactory(this.resolvePacketAdapter())
        .build();
    }

    // start the emote player
    this.startEmoteTask(false);
    // start the knock back task
    this.knockBackTask = this.scheduler.runTaskTimer(plugin, () -> {
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
                          LabyModExtension
                            .createEmotePacket(this.npcPlatform.packetFactory(), emote)
                            .schedule(player, npcSelector.handleNPC());
                        } else {
                          LabyModExtension
                            .createEmotePacket(this.npcPlatform.packetFactory(), emoteId)
                            .schedule(player, npcSelector.handleNPC());
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

  @Override
  public void initialize() {
    super.initialize();

    // spawn all npcs that are in chunks that were loaded before the plugin was enabled
    for (var entity : this.trackedEntities.values()) {
      if (entity.canSpawn()) {
        entity.spawn();
      }
    }
  }

  @Override
  protected @NonNull PlatformSelectorEntity<Location, Player, ItemStack, Inventory, Scoreboard> createSelectorEntity(
    @NonNull NPC base
  ) {
    return base.npcType() == NPC.NPCType.ENTITY
      ? new EntityBukkitPlatformSelectorEntity(base, this.plugin, this.server, this.scheduler, this.playerManager, this)
      : new NPCBukkitPlatformSelector(
        base,
        this.plugin,
        this.server,
        this.scheduler,
        this.playerManager,
        this,
        this.npcPlatform);
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
    var world = this.server.getWorld(position.world());
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
      && service.serviceId().environment().readProperty(ServiceEnvironmentType.JAVA_SERVER);
  }

  @Override
  public void handleInternalNPCConfigUpdate(@NonNull NPCConfiguration configuration) {
    super.handleInternalNPCConfigUpdate(configuration);
    // re-schedule the emote task if it's not yet running
    this.startEmoteTask(false);
  }

  public @NonNull Platform<World, Player, ItemStack, Plugin> npcPlatform() {
    return this.npcPlatform;
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
        this.npcEmoteTask = this.scheduler.runTaskLaterAsynchronously(this.plugin, () -> {
          // select an emote to play
          var emotes = ent.emoteConfiguration().emoteIds();
          var emoteId = this.randomEmoteId(ent.emoteConfiguration(), emotes);
          // check if we can select an emote
          if (emoteId >= -1) {
            // play the emote on each npc
            for (var npc : this.npcPlatform.npcTracker().trackedNpcs()) {
              if (emoteId == -1) {
                var emote = emotes[ThreadLocalRandom.current().nextInt(0, emotes.length)];
                LabyModExtension.createEmotePacket(this.npcPlatform.packetFactory(), emote).scheduleForTracked(npc);
              } else {
                LabyModExtension.createEmotePacket(this.npcPlatform.packetFactory(), emoteId).scheduleForTracked(npc);
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

  protected @NonNull PlatformPacketAdapter<World, Player, ItemStack, Plugin> resolvePacketAdapter() {
    var bukkitVersion = this.server.getBukkitVersion();
    var parsedVersion = PEVersion.fromString(bukkitVersion.substring(0, bukkitVersion.indexOf("-")));
    var latestPEVersion = PEVersion.fromString(ServerVersion.getLatest().getReleaseName());
    if (parsedVersion.isNewerThan(latestPEVersion)) {
      this.plugin.getLogger().info("NPCs using ProtocolLib for version " + bukkitVersion);
      return BukkitProtocolAdapter.protocolLib();
    }

    this.plugin.getLogger().info("NPCs using PacketEvents for version " + bukkitVersion);
    return BukkitProtocolAdapter.packetEvents();
  }
}
