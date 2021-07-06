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

package eu.cloudnetservice.cloudnet.ext.npcs.bukkit.listener;

import com.github.juliarn.npc.event.PlayerNPCInteractEvent;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.NPCAction;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.BukkitNPCManagement;
import eu.cloudnetservice.cloudnet.ext.npcs.bukkit.BukkitNPCProperties;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class NPCInventoryListener implements Listener {

  private static final Random RANDOM = new Random();

  private final BukkitNPCManagement npcManagement;

  private final IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry()
    .getFirstService(IPlayerManager.class);

  private final Map<Integer, BukkitNPCProperties> propertiesCache = new HashMap<>();

  public NPCInventoryListener(BukkitNPCManagement npcManagement) {
    this.npcManagement = npcManagement;
  }

  @EventHandler
  public void handleNPCInteract(PlayerNPCInteractEvent event) {
    if (event.getHand() != PlayerNPCInteractEvent.Hand.MAIN_HAND) {
      return;
    }

    Player player = event.getPlayer();
    int entityId = event.getNPC().getEntityId();

    BukkitNPCProperties properties = this.propertiesCache
      .computeIfAbsent(entityId, key -> this.npcManagement.getNPCProperties().values().stream()
        .filter(npcProperty -> npcProperty.getEntityId() == key)
        .findFirst()
        .orElse(null));

    if (properties != null) {
      CloudNPC cloudNPC = properties.getHolder();

      PlayerNPCInteractEvent.EntityUseAction action = event.getUseAction();

      if (action == PlayerNPCInteractEvent.EntityUseAction.INTERACT_AT
        || action == PlayerNPCInteractEvent.EntityUseAction.ATTACK) {
        NPCAction npcAction = action == PlayerNPCInteractEvent.EntityUseAction.INTERACT_AT
          ? cloudNPC.getRightClickAction()
          : cloudNPC.getLeftClickAction();

        if (npcAction == NPCAction.OPEN_INVENTORY) {
          player.openInventory(properties.getInventory());
        } else if (npcAction.name().startsWith("DIRECT")) {
          List<ServiceInfoSnapshot> services = this.npcManagement.filterNPCServices(cloudNPC).stream()
            .map(Pair::getFirst)
            .collect(Collectors.toList());

          if (services.size() > 0) {
            String targetServiceName = null;

            switch (npcAction) {
              case DIRECT_CONNECT_RANDOM:
                targetServiceName = services.get(RANDOM.nextInt(services.size())).getName();
                break;
              case DIRECT_CONNECT_LOWEST_PLAYERS:
                targetServiceName = services.stream()
                  .min(Comparator.comparingInt(
                    service -> service.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0)
                  ))
                  .map(ServiceInfoSnapshot::getName)
                  .orElse(null);
                break;
              case DIRECT_CONNECT_HIGHEST_PLAYERS:
                targetServiceName = services.stream()
                  .max(Comparator.comparingInt(
                    service -> service.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0)
                  ))
                  .map(ServiceInfoSnapshot::getName)
                  .orElse(null);
                break;
              default:
                break;
            }

            if (targetServiceName != null) {
              this.playerManager.getPlayerExecutor(player.getUniqueId()).connect(targetServiceName);
            }
          }
        }
      }


    }
  }

  @EventHandler
  public void handleInventoryClick(InventoryClickEvent event) {
    Inventory inventory = event.getClickedInventory();
    ItemStack currentItem = event.getCurrentItem();

    if (inventory != null && currentItem != null && inventory.getHolder() == null && event
      .getWhoClicked() instanceof Player) {
      this.npcManagement.getNPCProperties().values().stream()
        .filter(properties -> properties.getInventory().equals(inventory))
        .findFirst()
        .ifPresent(properties -> {
          event.setCancelled(true);
          int slot = event.getSlot();

          if (properties.getServerSlots().containsKey(slot)) {
            Player player = (Player) event.getWhoClicked();
            String serverName = properties.getServerSlots().get(slot);

            this.playerManager.getPlayerExecutor(player.getUniqueId()).connect(serverName);
          }
        });
    }
  }

}
