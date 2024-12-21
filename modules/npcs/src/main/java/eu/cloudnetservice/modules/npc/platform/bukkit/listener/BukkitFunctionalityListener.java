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

package eu.cloudnetservice.modules.npc.platform.bukkit.listener;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.event.AttackNpcEvent;
import com.github.juliarn.npclib.api.event.InteractNpcEvent;
import com.github.juliarn.npclib.api.event.ShowNpcEvent;
import com.github.juliarn.npclib.api.protocol.enums.EntityStatus;
import com.github.juliarn.npclib.api.protocol.enums.ItemSlot;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.github.juliarn.npclib.ext.labymod.LabyModExtension;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.platform.bukkit.BukkitPlatformNPCManagement;
import eu.cloudnetservice.modules.npc.platform.bukkit.entity.NPCBukkitPlatformSelector;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class BukkitFunctionalityListener implements Listener {

  private static final ItemSlot[] ITEM_SLOTS = ItemSlot.values();

  private final Plugin plugin;
  private final BukkitScheduler scheduler;
  private final BukkitPlatformNPCManagement management;

  @Inject
  public BukkitFunctionalityListener(
    @NonNull Plugin plugin,
    @NonNull BukkitScheduler scheduler,
    @NonNull BukkitPlatformNPCManagement management
  ) {
    this.plugin = plugin;
    this.scheduler = scheduler;
    this.management = management;

    var bus = management.npcPlatform().eventManager();
    bus.registerEventHandler(AttackNpcEvent.class, this::handleNpcAttack);
    bus.registerEventHandler(InteractNpcEvent.class, this::handleNpcInteract);
    bus.registerEventHandler(ShowNpcEvent.Post.class, this::handleNpcShow);
  }

  public void handleNpcShow(@NonNull ShowNpcEvent.Post event) {
    var npc = event.npc();
    var player = event.player();

    // send out the head rotation that was used when spawning the npc
    // unless the npc is configured to look directly at the player anyway
    if (!npc.flagValueOrDefault(Npc.LOOK_AT_PLAYER)) {
      var requestedPosition = npc.position();
      npc.rotate(requestedPosition.yaw(), requestedPosition.pitch()).schedule(player);
    }

    // enable all skin players
    npc.changeMetadata(EntityMetadataFactory.skinLayerMetaFactory(), true).schedule(player);

    // applies the settings made to the selector entity (the stored CloudNet entity)
    npc.flagValue(NPCBukkitPlatformSelector.SELECTOR_ENTITY).ifPresent(selectorEntity -> {
      // applies the entity status: glowing, on fire, flying with elytra, ...
      npc
        .changeMetadata(EntityMetadataFactory.entityStatusMetaFactory(), this.collectEntityStatus(selectorEntity.npc()))
        .schedule(player);

      // applies the items (in hands & armor)
      var entries = selectorEntity.npc().items().entrySet();
      for (var entry : entries) {
        if (entry.getKey() >= 0 && entry.getKey() <= 5) {
          var item = new ItemStack(Material.matchMaterial(entry.getValue()));
          npc.changeItem(ITEM_SLOTS[entry.getKey()], item).schedule(player);
        }
      }
    });
  }

  public void handleNpcAttack(@NonNull AttackNpcEvent event) {
    this.scheduler.runTask(
      this.plugin,
      () -> this.handleClick(event.player(), null, event.npc().entityId(), true));
  }

  public void handleNpcInteract(@NonNull InteractNpcEvent event) {
    this.scheduler.runTask(
      this.plugin,
      () -> this.handleClick(event.player(), null, event.npc().entityId(), false));
  }

  @EventHandler
  public void handle(@NonNull PlayerInteractEntityEvent event) {
    this.handleClick(event.getPlayer(), event, event.getRightClicked().getEntityId(), false);
  }

  @EventHandler(ignoreCancelled = true)
  public void handle(@NonNull EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player damager) {
      this.handleClick(damager, event, event.getEntity().getEntityId(), true);
    }
  }

  @EventHandler
  public void handle(@NonNull InventoryClickEvent event) {
    var item = event.getCurrentItem();
    var inv = event.getClickedInventory();
    var clicker = event.getWhoClicked();
    // check if we can handle the event
    if (item != null && item.hasItemMeta() && inv != null && inv.getHolder() == null && clicker instanceof Player) {
      this.management.trackedEntities().values().stream()
        .filter(npc -> inv.equals(npc.selectorInventory()))
        .findFirst()
        .ifPresent(npc -> {
          event.setCancelled(true);
          npc.handleInventoryInteract(inv, (Player) clicker, item);
        });
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void handle(@NonNull PlayerJoinEvent event) {
    var player = event.getPlayer();

    // create a new scoreboard for the player if the player uses the main scoreboard
    var manager = player.getServer().getScoreboardManager();
    if (manager != null && player.getScoreboard().equals(manager.getMainScoreboard())) {
      player.setScoreboard(manager.getNewScoreboard());
    }

    // we have to register each entity to the players scoreboard
    for (var entity : this.management.trackedEntities().values()) {
      if (entity.spawned()) {
        entity.registerScoreboardTeam(player.getScoreboard());
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void playOnJoinEmoteIds(@NonNull PlayerJoinEvent event) {
    var entry = this.management.applicableNPCConfigurationEntry();
    if (entry != null) {
      var onJoinEmoteIds = entry.emoteConfiguration().onJoinEmoteIds();
      var selectedNpcId = this.management.randomEmoteId(entry.emoteConfiguration(), onJoinEmoteIds);
      // check if an emote id could be selected
      if (selectedNpcId >= -1) {
        // play the emote to all npcs
        for (var npc : this.management.npcPlatform().npcTracker().trackedNpcs()) {
          // verify that the player *could* see the emote
          if (npc.position().worldId().equals(event.getPlayer().getWorld().getName())) {
            // check if the emote id is fixed
            if (selectedNpcId != -1) {
              LabyModExtension
                .createEmotePacket(this.management.npcPlatform().packetFactory())
                .schedule(event.getPlayer(), npc);
            } else {
              var randomEmote = onJoinEmoteIds[ThreadLocalRandom.current().nextInt(0, onJoinEmoteIds.length)];
              LabyModExtension
                .createEmotePacket(this.management.npcPlatform().packetFactory(), randomEmote)
                .schedule(event.getPlayer(), npc);
            }
          }
        }
      }
    }
  }

  private @NonNull Collection<EntityStatus> collectEntityStatus(@NonNull NPC npc) {
    Collection<EntityStatus> status = new HashSet<>();
    if (npc.glowing()) {
      status.add(EntityStatus.GLOWING);
    }

    if (npc.flyingWithElytra()) {
      status.add(EntityStatus.FLYING_WITH_ELYTRA);
    }

    if (npc.burning()) {
      status.add(EntityStatus.ON_FIRE);
    }

    return status;
  }

  private void handleClick(@NonNull Player player, @Nullable Cancellable cancellable, int entityId, boolean left) {
    this.management.trackedEntities().values().stream()
      .filter(npc -> npc.entityId() == entityId)
      .findFirst()
      .ifPresent(entity -> {
        // cancel the event if needed
        if (cancellable != null) {
          cancellable.setCancelled(true);
        }
        // handle click
        if (left) {
          entity.handleLeftClickAction(player);
        } else {
          entity.handleRightClickAction(player);
        }
      });
  }
}
