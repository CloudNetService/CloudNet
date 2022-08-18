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

package eu.cloudnetservice.modules.npc.platform.bukkit.listener;

import com.github.juliarn.npclib.api.event.AttackNpcEvent;
import com.github.juliarn.npclib.api.event.InteractNpcEvent;
import com.github.juliarn.npclib.api.event.ShowNpcEvent;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import eu.cloudnetservice.modules.npc.platform.bukkit.BukkitPlatformNPCManagement;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

public final class BukkitFunctionalityListener implements Listener {

  // See: https://wiki.vg/Entity_metadata#Entity
  private static final byte GLOWING_FLAGS = 1 << 6;
  private static final byte ELYTRA_FLYING_FLAGS = (byte) (1 << 7);
  private static final byte FLYING_AND_GLOWING = (byte) (GLOWING_FLAGS | ELYTRA_FLYING_FLAGS);
  /*
  .spawnCustomizer((spawnedNpc, player) -> {
        // just because the client is stupid sometimes
        spawnedNpc.rotation().queueRotate(this.npcLocation.getYaw(), this.npcLocation.getPitch()).send(player);
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
      }).build(this.platform);
   */

  private final BukkitPlatformNPCManagement management;

  public BukkitFunctionalityListener(@NonNull BukkitPlatformNPCManagement management) {
    this.management = management;
    var bus = management.npcPlatform().eventBus();
    bus.subscribe(AttackNpcEvent.class, this::handleNpcAttack);
    bus.subscribe(InteractNpcEvent.class, this::handleNpcInteract);
    bus.subscribe(ShowNpcEvent.Pre.class, this::handleNpcShow);
  }

  public void handleNpcShow(@NonNull ShowNpcEvent.Pre event) {
    var packetFactory = event.npc().platform().packetFactory();
    packetFactory.createEntityMetaPacket(true, EntityMetadataFactory.skinLayerMetaFactory())
      .scheduleForTracked(event.npc());



  }

  public void handleNpcAttack(@NonNull AttackNpcEvent event) {
    this.handleClick(event.player(), null, event.npc().entityId(), true);
  }

  public void handleNpcInteract(@NonNull InteractNpcEvent event) {
    this.handleClick(event.player(), null, event.npc().entityId(), false);
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
        .filter(npc -> npc.selectorInventory().equals(inv))
        .findFirst()
        .ifPresent(npc -> {
          event.setCancelled(true);
          npc.handleInventoryInteract(inv, (Player) clicker, item);
        });
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handle(@NonNull PlayerJoinEvent event) {
    event.getPlayer().setScoreboard(this.management.scoreboard());
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
          if (npc.getLocation().getWorld().getUID().equals(event.getPlayer().getWorld().getUID())) {
            // check if the emote id is fixed
            if (selectedNpcId != -1) {
              npc.labymod().queue(LabyModModifier.LabyModAction.EMOTE, selectedNpcId).send(event.getPlayer());
            } else {
              var randomEmote = onJoinEmoteIds[ThreadLocalRandom.current().nextInt(0, onJoinEmoteIds.length)];
              npc.labymod().queue(LabyModModifier.LabyModAction.EMOTE, randomEmote).send(event.getPlayer());
            }
          }
        }
      }
    }
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
