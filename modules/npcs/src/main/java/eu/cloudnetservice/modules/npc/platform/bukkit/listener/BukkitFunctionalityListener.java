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
import com.github.juliarn.npclib.api.protocol.enums.ItemSlot;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.github.juliarn.npclib.ext.labymod.LabyModExtension;
import eu.cloudnetservice.modules.npc.platform.bukkit.BukkitPlatformNPCManagement;
import eu.cloudnetservice.modules.npc.platform.bukkit.entity.NPCBukkitPlatformSelector;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
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
import org.jetbrains.annotations.Nullable;

public final class BukkitFunctionalityListener implements Listener {

  // See: https://wiki.vg/Entity_metadata#Entity
  private static final byte GLOWING_FLAGS = 1 << 6;
  private static final byte ELYTRA_FLYING_FLAGS = (byte) (1 << 7);
  private static final byte FLYING_AND_GLOWING = (byte) (GLOWING_FLAGS | ELYTRA_FLYING_FLAGS);

  private static final EntityMetadataFactory<Byte, Byte> ENTITY_EFFECT_FACTORY = EntityMetadataFactory.<Byte, Byte>metaFactoryBuilder()
    .baseIndex(0)
    .type(Byte.class)
    .inputConverter(Function.identity())
    .availabilityChecker(versionAccessor -> versionAccessor.atLeast(1, 9, 0))
    .build();

  private static final ItemSlot[] ITEM_SLOTS = ItemSlot.values();

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
    event.npc().flagValue(NPCBukkitPlatformSelector.SELECTOR_ENTITY).ifPresent(selectorEntity -> {
      if (selectorEntity.npc().glowing() && selectorEntity.npc().flyingWithElytra()) {
        packetFactory.createEntityMetaPacket(FLYING_AND_GLOWING, ENTITY_EFFECT_FACTORY).scheduleForTracked(event.npc());
      } else if (selectorEntity.npc().glowing()) {
        packetFactory.createEntityMetaPacket(GLOWING_FLAGS, ENTITY_EFFECT_FACTORY).scheduleForTracked(event.npc());
      } else if (selectorEntity.npc().flyingWithElytra()) {
        packetFactory.createEntityMetaPacket(ELYTRA_FLYING_FLAGS, ENTITY_EFFECT_FACTORY).scheduleForTracked(event.npc());
      }
      var entries = selectorEntity.npc().items().entrySet();
      for (var entry : entries) {
        if (entry.getKey() >= 0 && entry.getKey() <= 5) {
          var item = new ItemStack(Material.matchMaterial(entry.getValue()));
          packetFactory.createEquipmentPacket(ITEM_SLOTS[entry.getKey()], item).scheduleForTracked(event.npc());
        }
      }
    });
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
          if (npc.position().worldId().equals(event.getPlayer().getWorld().getName())) {
            // check if the emote id is fixed
            if (selectedNpcId != -1) {
              LabyModExtension.createEmotePacket(this.management.npcPlatform().packetFactory())
                .schedule(event.getPlayer(), npc);
            } else {
              var randomEmote = onJoinEmoteIds[ThreadLocalRandom.current().nextInt(0, onJoinEmoteIds.length)];
              LabyModExtension.createEmotePacket(this.management.npcPlatform().packetFactory(), randomEmote)
                .schedule(event.getPlayer(), npc);
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
