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

    this.startEmoteTask(false);
    this.knockBackTask = this.scheduler.runTaskTimer(plugin, new BukkitNPCKnockbackTask(this), 20, 5);
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
    this.startEmoteTask(false);
  }

  public @NonNull Platform<World, Player, ItemStack, Plugin> npcPlatform() {
    return this.npcPlatform;
  }

  protected void startEmoteTask(boolean force) {
    var currentTask = this.npcEmoteTask;
    if (currentTask == null || force) {
      if (currentTask != null) {
        // always cancel the current task, even if we're not going to restart it
        currentTask.cancel();
        this.npcEmoteTask = null;
      }

      var configEntry = this.applicableNPCConfigurationEntry();
      if (configEntry == null) {
        return;
      }

      // -1 is used to indicate a random emote, positive value a fixed emote id
      var labyModEmotes = configEntry.emoteConfiguration().emoteIds();
      var emoteId = this.randomEmoteId(configEntry.emoteConfiguration(), labyModEmotes);
      if (emoteId < -1) {
        return;
      }

      var emoteConfig = configEntry.emoteConfiguration();
      var minDelayTicks = emoteConfig.minEmoteDelayTicks();
      var maxDelayTicks = emoteConfig.maxEmoteDelayTicks();
      if (minDelayTicks <= 0) {
        return;
      }

      var delayTicks = maxDelayTicks > minDelayTicks
        ? ThreadLocalRandom.current().nextLong(minDelayTicks, maxDelayTicks)
        : minDelayTicks;
      this.npcEmoteTask = this.scheduler.runTaskLaterAsynchronously(this.plugin, () -> {
        var random = ThreadLocalRandom.current();
        for (var entity : this.trackedEntities.values()) {
          if (entity.spawned() && entity instanceof NPCBukkitPlatformSelector npcSelector) {
            var libNpc = npcSelector.handleNPC();
            var emote = switch (emoteId) {
              case -1 -> labyModEmotes[random.nextInt(0, labyModEmotes.length)];
              case int id -> id;
            };
            LabyModExtension.createEmotePacket(this.npcPlatform.packetFactory(), emote).scheduleForTracked(libNpc);
          }
        }

        // re-schedule the task to send the next emote with a new delay
        this.startEmoteTask(true);
      }, delayTicks);
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
