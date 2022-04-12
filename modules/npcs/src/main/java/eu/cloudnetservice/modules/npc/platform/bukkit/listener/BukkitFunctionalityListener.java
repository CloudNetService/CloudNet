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

import com.github.juliarn.npc.event.PlayerNPCInteractEvent;
import com.github.juliarn.npc.event.PlayerNPCInteractEvent.EntityUseAction;
import com.github.juliarn.npc.event.PlayerNPCInteractEvent.Hand;
import com.github.juliarn.npc.modifier.LabyModModifier.LabyModAction;
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

  private final BukkitPlatformNPCManagement management;

  public BukkitFunctionalityListener(@NonNull BukkitPlatformNPCManagement management) {
    this.management = management;
  }

  @EventHandler
  public void handle(@NonNull PlayerNPCInteractEvent event) {
    if (event.getHand() == Hand.MAIN_HAND && event.getUseAction() != EntityUseAction.INTERACT_AT) {
      this.handleClick(
        event.getPlayer(),
        null,
        event.getNPC().getEntityId(),
        event.getUseAction() == EntityUseAction.ATTACK);
    }
  }

  @EventHandler
  public void handle(@NonNull PlayerInteractEntityEvent event) {
    this.handleClick(
      event.getPlayer(),
      event,
      event.getRightClicked().getEntityId(),
      false);
  }

  @EventHandler(ignoreCancelled = true)
  public void handle(@NonNull EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player damager) {
      this.handleClick(
        damager,
        event,
        event.getEntity().getEntityId(),
        true);
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
        for (var npc : this.management.npcPool().getNPCs()) {
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
