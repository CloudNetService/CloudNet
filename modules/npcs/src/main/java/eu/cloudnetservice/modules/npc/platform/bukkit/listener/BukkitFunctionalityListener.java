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

package eu.cloudnetservice.modules.npc.platform.bukkit.listener;

import com.github.juliarn.npc.NPC;
import com.github.juliarn.npc.event.PlayerNPCInteractEvent;
import com.github.juliarn.npc.event.PlayerNPCInteractEvent.EntityUseAction;
import com.github.juliarn.npc.event.PlayerNPCInteractEvent.Hand;
import com.github.juliarn.npc.modifier.LabyModModifier.LabyModAction;
import eu.cloudnetservice.modules.npc.configuration.NPCConfigurationEntry;
import eu.cloudnetservice.modules.npc.platform.bukkit.BukkitPlatformNPCManagement;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BukkitFunctionalityListener implements Listener {

  private final BukkitPlatformNPCManagement management;

  public BukkitFunctionalityListener(@NotNull BukkitPlatformNPCManagement management) {
    this.management = management;
  }

  @EventHandler
  public void handle(@NotNull PlayerNPCInteractEvent event) {
    if (event.getHand() == Hand.MAIN_HAND && event.getUseAction() != EntityUseAction.INTERACT_AT) {
      this.handleClick(
        event.getPlayer(),
        null,
        event.getNPC().getEntityId(),
        event.getUseAction() == EntityUseAction.ATTACK);
    }
  }

  @EventHandler
  public void handle(@NotNull PlayerInteractEntityEvent event) {
    this.handleClick(
      event.getPlayer(),
      event,
      event.getRightClicked().getEntityId(),
      false);
  }

  @EventHandler(ignoreCancelled = true)
  public void handle(@NotNull EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player) {
      this.handleClick(
        (Player) event.getDamager(),
        event,
        event.getEntity().getEntityId(),
        true);
    }
  }

  @EventHandler
  public void handle(@NotNull InventoryClickEvent event) {
    var item = event.getCurrentItem();
    var inv = event.getClickedInventory();
    var clicker = event.getWhoClicked();
    // check if we can handle the event
    if (item != null && item.hasItemMeta() && inv != null && inv.getHolder() == null && clicker instanceof Player) {
      this.management.getTrackedEntities().values().stream()
        .filter(npc -> npc.getSelectorInventory().equals(inv))
        .findFirst()
        .ifPresent(npc -> {
          event.setCancelled(true);
          npc.handleInventoryInteract(inv, (Player) clicker, item);
        });
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handle(@NotNull PlayerJoinEvent event) {
    event.getPlayer().setScoreboard(this.management.getScoreboard());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void playOnJoinEmoteIds(@NotNull PlayerJoinEvent event) {
    var entry = this.management.getApplicableNPCConfigurationEntry();
    if (entry != null) {
      var onJoinEmoteIds = entry.getEmoteConfiguration().getOnJoinEmoteIds();
      var selectedNpcId = this.management.getRandomEmoteId(entry.getEmoteConfiguration(), onJoinEmoteIds);
      // check if an emote id could be selected
      if (selectedNpcId >= -1) {
        // play the emote to all npcs
        for (var npc : this.management.getNpcPool().getNPCs()) {
          // verify that the player *could* see the emote
          if (npc.getLocation().getWorld().getUID().equals(event.getPlayer().getWorld().getUID())) {
            // check if the emote id is fixed
            if (selectedNpcId != -1) {
              npc.labymod().queue(LabyModAction.EMOTE, selectedNpcId).send(event.getPlayer());
            } else {
              var randomEmote = onJoinEmoteIds[ThreadLocalRandom.current().nextInt(0, onJoinEmoteIds.length)];
              npc.labymod().queue(LabyModAction.EMOTE, randomEmote).send(event.getPlayer());
            }
          }
        }
      }
    }
  }

  private void handleClick(@NotNull Player player, @Nullable Cancellable cancellable, int entityId, boolean left) {
    management.getTrackedEntities().values().stream()
      .filter(npc -> npc.getEntityId() == entityId)
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
