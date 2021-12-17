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

package eu.cloudnetservice.modules.npc.platform.bukkit.entity;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperties;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.NPC.ClickAction;
import eu.cloudnetservice.modules.npc.configuration.InventoryConfiguration;
import eu.cloudnetservice.modules.npc.configuration.ItemLayout;
import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import eu.cloudnetservice.modules.npc.platform.bukkit.BukkitPlatformNPCManagement;
import eu.cloudnetservice.modules.npc.platform.bukkit.util.ReflectionUtil;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BukkitPlatformSelectorEntity
  implements PlatformSelectorEntity<Location, Player, ItemStack, Inventory> {

  protected static final MethodHandle SET_COLOR = ReflectionUtil.findMethod(
    Team.class,
    "setColor",
    ChatColor.class);

  protected final NPC npc;
  protected final Plugin plugin;
  protected final Location npcLocation;
  protected final BukkitPlatformNPCManagement npcManagement;
  protected final Set<Integer> infoLineEntityIds = new HashSet<>();
  protected final Set<InfoLineWrapper> infoLines = new HashSet<>();
  protected final Map<UUID, ServiceItemWrapper> serviceItems = new LinkedHashMap<>();

  protected volatile Inventory inventory;

  protected BukkitPlatformSelectorEntity(
    @NotNull BukkitPlatformNPCManagement npcManagement,
    @NotNull Plugin plugin,
    @NotNull NPC npc
  ) {
    this.npc = npc;
    this.plugin = plugin;
    this.npcManagement = npcManagement;
    this.npcLocation = npcManagement.toPlatformLocation(npc.getLocation());
  }

  @Override
  public void spawn() {
    Bukkit.getScheduler().runTask(this.plugin, () -> {
      // create the inventory view
      this.rebuildInventory(this.npcManagement.getInventoryConfiguration());
      // spawn the selector entity
      this.spawn0();
      // check if the entity should have a glowing color
      if (SET_COLOR != null && this.npc.isGlowing()) {
        var scoreboard = this.npcManagement.getScoreboard();
        // check if a team for the glowing color is already registered
        var team = scoreboard.getTeam("nc" + this.npc.getGlowingColor());
        if (team == null) {
          team = scoreboard.registerNewTeam("nc" + this.npc.getGlowingColor());
          // try to set the team color
          var color = ChatColor.getByChar(this.npc.getGlowingColor());
          if (color != null) {
            try {
              SET_COLOR.invoke(team, color);
            } catch (Throwable throwable) {
              throw new IllegalStateException("Unable to set team color", throwable);
            }
          }
        }
        // register the spawned entity to the team
        team.addEntry(this.getScoreboardRepresentation());
        // let the entity glow!
        this.addGlowingEffect();
      }
      // spawn the info lines
      for (var i = npc.getInfoLines().size() - 1; i >= 0; i--) {
        var armorStand = (ArmorStand) this.npcLocation.getWorld().spawnEntity(
          this.npcLocation.clone().add(0, this.getHeightAddition(i), 0),
          EntityType.ARMOR_STAND);
        armorStand.setSmall(true);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setBasePlate(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomNameVisible(true);
        // if it is the top info line try to spawn the item above it
        if (i == npc.getInfoLines().size() - 1) {
          var materialName = this.npc.getFloatingItem();
          if (materialName != null) {
            var material = Material.matchMaterial(materialName);
            if (material != null) {
              var item = this.npcLocation.getWorld().dropItem(armorStand.getLocation(), new ItemStack(material));
              item.setTicksLived(Integer.MAX_VALUE);
              item.setPickupDelay(Integer.MAX_VALUE);
              // set the passenger
              armorStand.setPassenger(item);
            }
          }
        }
        // register the info line
        var wrapper = new InfoLineWrapper(this.npc.getInfoLines().get(i), armorStand);
        wrapper.rebuildInfoLine();
        // register the line
        this.infoLines.add(wrapper);
        this.infoLineEntityIds.add(armorStand.getEntityId());
      }
    });
  }

  @Override
  public void remove() {
    Bukkit.getScheduler().runTask(this.plugin, () -> {
      for (var infoLine : this.infoLines) {
        // remove the armor stand passenger (if any)
        var passenger = infoLine.armorStand.getPassenger();
        if (passenger != null) {
          passenger.remove();
        }
        // remove the info line armor stand
        infoLine.armorStand.remove();
      }
      this.infoLines.clear();
      this.infoLineEntityIds.clear();
      // remove the actual selector npc
      this.remove0();
    });
  }

  @Override
  public void update() {
    // rebuild all items - we can do that async
    this.serviceItems.values().forEach(wrapper -> this.trackService(wrapper.getService()));
    // rebuild everything else sync
    Bukkit.getScheduler().runTask(this.plugin, () -> {
      this.rebuildInventory(this.npcManagement.getInventoryConfiguration());
      this.rebuildInfoLines();
    });
  }

  @Override
  public void trackService(@NotNull ServiceInfoSnapshot service) {
    // get the current item
    var wrapper = this.serviceItems.get(service.serviceId().uniqueId());
    // build the item for the service
    var configuration = this.npcManagement.getInventoryConfiguration();
    var layouts = configuration.getHolder(service.configuration().groups().toArray(new String[0]));
    // get the service state
    var state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(service);
    ItemLayout layout;
    switch (state) {
      case EMPTY_ONLINE:
        layout = layouts.emptyLayout();
        break;
      case FULL_ONLINE:
        if (configuration.showFullServices()) {
          layout = layouts.fullLayout();
          break;
        } else {
          return;
        }
      case ONLINE:
        layout = layouts.onlineLayout();
        break;
      default:
        return;
    }
    // build the item stack from the layout
    var item = this.buildItemStack(layout, service);
    if (item != null) {
      if (wrapper == null) {
        // store a new wrapper
        this.serviceItems.put(service.serviceId().uniqueId(), new ServiceItemWrapper(item, service));
      } else {
        // update the item wrapper
        wrapper.setItemStack(item);
        wrapper.setService(service);
      }
      // push the service update
      this.rebuildInfoLines();
      this.rebuildInventory(configuration);
    } else if (wrapper != null) {
      // unable to build a new item - remove the current one
      this.serviceItems.remove(service.serviceId().uniqueId());
    }
  }

  @Override
  public void stopTrackingService(@NotNull ServiceInfoSnapshot service) {
    Bukkit.getScheduler().runTask(this.plugin, () -> {
      // get the old item wrapper
      var wrapper = this.serviceItems.remove(service.serviceId().uniqueId());
      if (wrapper != null) {
        // the service got tracked before - rebuild the inventory and info lines
        this.rebuildInfoLines();
        this.rebuildInventory(this.npcManagement.getInventoryConfiguration());
      }
    });
  }

  @Override
  public void handleLeftClickAction(@NotNull Player player) {
    this.handleClickAction(player, this.npc.getLeftClickAction());
  }

  @Override
  public void handleRightClickAction(@NotNull Player player) {
    this.handleClickAction(player, this.npc.getRightClickAction());
  }

  @Override
  public void handleInventoryInteract(@NotNull Inventory inv, @NotNull Player player, @NotNull ItemStack clickedItem) {
    // find the server associated with the clicked item
    for (var wrapper : this.serviceItems.values()) {
      if (wrapper.getItemStack().equals(clickedItem)) {
        // close the inventory
        player.closeInventory();
        // connect the player
        this.getPlayerManager().getPlayerExecutor(player.getUniqueId()).connect(wrapper.getService().name());
        break;
      }
    }
  }

  @Override
  public @NotNull Inventory getSelectorInventory() {
    return this.inventory;
  }

  @Override
  public @NotNull Set<Integer> getInfoLineEntityIds() {
    return this.infoLineEntityIds;
  }

  @Override
  public @NotNull NPC getNPC() {
    return this.npc;
  }

  @Override
  public @NotNull Location getLocation() {
    return this.npcLocation;
  }

  @Override
  public boolean canSpawn() {
    var chunkX = NumberConversions.floor(this.npcLocation.getX()) >> 4;
    var chunkZ = NumberConversions.floor(this.npcLocation.getZ()) >> 4;

    return this.npcLocation.getWorld() != null && this.npcLocation.getWorld().isChunkLoaded(chunkX, chunkZ);
  }

  protected void handleClickAction(@NotNull Player player, @NotNull ClickAction action) {
    switch (action) {
      case OPEN_INVENTORY:
        player.openInventory(this.inventory);
        break;
      case DIRECT_CONNECT_RANDOM: {
        List<ServiceItemWrapper> wrappers = new ArrayList<>(this.serviceItems.values());
        // connect the player to the first element if present
        if (!wrappers.isEmpty()) {
          var wrapper = wrappers.get(ThreadLocalRandom.current().nextInt(0, wrappers.size()));
          this.getPlayerManager().getPlayerExecutor(player.getUniqueId()).connect(wrapper.getService().name());
        }
      }
      break;
      case DIRECT_CONNECT_LOWEST_PLAYERS: {
        this.serviceItems.values().stream()
          .map(ServiceItemWrapper::getService)
          .min(Comparator.comparingInt(service -> BridgeServiceProperties.ONLINE_COUNT.read(service).orElse(0)))
          .ifPresent(ser -> this.getPlayerManager().getPlayerExecutor(player.getUniqueId()).connect(ser.name()));
      }
      break;
      case DIRECT_CONNECT_HIGHEST_PLAYERS: {
        this.serviceItems.values().stream()
          .map(ServiceItemWrapper::getService)
          .max(Comparator.comparingInt(service -> BridgeServiceProperties.ONLINE_COUNT.read(service).orElse(0)))
          .ifPresent(ser -> this.getPlayerManager().getPlayerExecutor(player.getUniqueId()).connect(ser.name()));
      }
      break;
      default:
      case NOTHING:
        break;
    }
  }

  protected @Nullable ItemStack buildItemStack(@NotNull ItemLayout layout, @Nullable ServiceInfoSnapshot service) {
    var material = Material.matchMaterial(layout.material());
    if (material != null) {
      var item = layout.subId() == -1
        ? new ItemStack(material)
        : new ItemStack(material, 1, (byte) layout.subId());
      // apply the meta
      var meta = item.getItemMeta();
      if (meta != null) {
        meta.setDisplayName(BridgeServiceHelper.fillCommonPlaceholders(
          layout.displayName(),
          this.npc.getTargetGroup(),
          service));
        meta.setLore(layout.lore().stream()
          .map(line -> BridgeServiceHelper.fillCommonPlaceholders(line, this.npc.getTargetGroup(), service))
          .collect(Collectors.toList()));
        // set the meta again
        item.setItemMeta(meta);
      }
      // done
      return item;
    }
    // unable to build the item
    return null;
  }

  protected void rebuildInfoLines() {
    this.infoLines.forEach(InfoLineWrapper::rebuildInfoLine);
  }

  protected void rebuildInventory(@NotNull InventoryConfiguration configuration) {
    // calculate the inventory size
    var inventorySize = configuration.inventorySize();
    if (configuration.dynamicSize()) {
      inventorySize = this.serviceItems.size();
      // try to make it to the next higher possible inventory size
      while (inventorySize == 0 || (inventorySize < 54 && inventorySize % 9 != 0)) {
        inventorySize++;
      }
    }
    // create the inventory
    var inventory = this.inventory;
    if (inventory == null || inventory.getSize() != inventorySize) {
      this.inventory = inventory = Bukkit.createInventory(null, inventorySize, this.npc.getDisplayName());
    }
    // remove all current contents
    inventory.clear();
    // add the fixed items
    for (var entry : configuration.fixedItems().entrySet()) {
      // check if the item would exceed the inventory size
      if (entry.getKey() < inventorySize) {
        // build and set the item
        var item = this.buildItemStack(entry.getValue(), null);
        if (item != null) {
          inventory.setItem(entry.getKey(), item);
        }
      }
    }
    // add the service items
    for (var wrapper : this.serviceItems.values()) {
      if (!inventory.addItem(wrapper.getItemStack()).isEmpty()) {
        // the inventory is full
        break;
      }
    }
  }

  protected @NotNull IPlayerManager getPlayerManager() {
    return CloudNetDriver.instance().servicesRegistry().firstService(IPlayerManager.class);
  }

  protected double getHeightAddition(int lineNumber) {
    var entry = this.npcManagement.getApplicableNPCConfigurationEntry();
    return entry == null ? lineNumber : entry.infoLineDistance() * lineNumber;
  }

  protected abstract void spawn0();

  protected abstract void remove0();

  protected abstract void addGlowingEffect();

  protected static final class ServiceItemWrapper {

    private volatile ItemStack itemStack;
    private volatile ServiceInfoSnapshot serviceInfoSnapshot;

    public ServiceItemWrapper(@NotNull ItemStack itemStack, @NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
      this.itemStack = itemStack;
      this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public @NotNull ItemStack getItemStack() {
      return this.itemStack;
    }

    public void setItemStack(@NotNull ItemStack itemStack) {
      this.itemStack = itemStack;
    }

    public @NotNull ServiceInfoSnapshot getService() {
      return this.serviceInfoSnapshot;
    }

    public void setService(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
      this.serviceInfoSnapshot = serviceInfoSnapshot;
    }
  }

  protected final class InfoLineWrapper {

    private final String basedInfoLine;
    private final ArmorStand armorStand;

    public InfoLineWrapper(String basedInfoLine, ArmorStand armorStand) {
      this.basedInfoLine = basedInfoLine;
      this.armorStand = armorStand;
    }

    private void rebuildInfoLine() {
      var npc = BukkitPlatformSelectorEntity.this.npc;
      // update based on the tracked services
      var tracked = BukkitPlatformSelectorEntity.this.serviceItems.values();
      // general info
      var onlinePlayers = Integer.toString(tracked.stream()
        .map(ServiceItemWrapper::getService)
        .mapToInt(snapshot -> BridgeServiceProperties.ONLINE_COUNT.read(snapshot).orElse(0))
        .sum());
      var maxPlayers = Integer.toString(tracked.stream()
        .map(ServiceItemWrapper::getService)
        .mapToInt(snapshot -> BridgeServiceProperties.MAX_PLAYERS.read(snapshot).orElse(0))
        .sum());
      var onlineServers = Integer.toString(tracked.size());
      // rebuild the info line
      var newInfoLine = this.basedInfoLine
        .replace("%group%", npc.getTargetGroup()).replace("%g%", npc.getTargetGroup())
        .replace("%online_players%", onlinePlayers).replace("%o_p%", onlinePlayers)
        .replace("%max_players%", maxPlayers).replace("%m_p%", maxPlayers)
        .replace("%online_servers%", onlineServers).replace("%o_s%", onlineServers);
      // set the custom name of the armor stand
      this.armorStand.setCustomName(newInfoLine);
    }
  }
}
