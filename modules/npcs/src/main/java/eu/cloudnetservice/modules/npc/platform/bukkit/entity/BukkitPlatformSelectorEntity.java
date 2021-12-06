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
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper.ServiceInfoState;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperties;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.NPC.ClickAction;
import eu.cloudnetservice.modules.npc.configuration.InventoryConfiguration;
import eu.cloudnetservice.modules.npc.configuration.InventoryConfiguration.ItemLayoutHolder;
import eu.cloudnetservice.modules.npc.configuration.ItemLayout;
import eu.cloudnetservice.modules.npc.configuration.NPCConfigurationEntry;
import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import eu.cloudnetservice.modules.npc.platform.bukkit.BukkitPlatformNPCManagement;
import eu.cloudnetservice.modules.npc.platform.bukkit.util.ReflectionUtil;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
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
  protected final Map<ServiceInfoSnapshot, ItemStack> serviceItems = new ConcurrentHashMap<>();

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
        Scoreboard scoreboard = this.npcManagement.getScoreboard();
        // check if a team for the glowing color is already registered
        Team team = scoreboard.getTeam("nc" + this.npc.getGlowingColor());
        if (team == null) {
          team = scoreboard.registerNewTeam("nc" + this.npc.getGlowingColor());
          // try to set the team color
          ChatColor color = ChatColor.getByChar(this.npc.getGlowingColor());
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
      for (int i = npc.getInfoLines().size() - 1; i >= 0; i--) {
        ArmorStand armorStand = (ArmorStand) this.npcLocation.getWorld().spawnEntity(
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
          String materialName = this.npc.getFloatingItem();
          if (materialName != null) {
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
              Item item = this.npcLocation.getWorld().dropItem(armorStand.getLocation(), new ItemStack(material));
              item.setTicksLived(Integer.MAX_VALUE);
              item.setPickupDelay(Integer.MAX_VALUE);
              // set the passenger
              armorStand.setPassenger(item);
            }
          }
        }
        // register the info line
        InfoLineWrapper wrapper = new InfoLineWrapper(this.npc.getInfoLines().get(i), armorStand);
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
      for (InfoLineWrapper infoLine : this.infoLines) {
        // remove the armor stand passenger (if any)
        Entity passenger = infoLine.armorStand.getPassenger();
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
    this.serviceItems.keySet().forEach(this::trackService);
    // rebuild everything else sync
    Bukkit.getScheduler().runTask(this.plugin, () -> {
      this.rebuildInventory(this.npcManagement.getInventoryConfiguration());
      this.rebuildInfoLines();
    });
  }

  @Override
  public void trackService(@NotNull ServiceInfoSnapshot service) {
    // remove the current item
    for (ServiceInfoSnapshot snapshot : this.serviceItems.keySet()) {
      if (snapshot.getServiceId().getUniqueId().equals(service.getServiceId().getUniqueId())) {
        this.serviceItems.remove(snapshot);
        break;
      }
    }
    // build the item for the service
    InventoryConfiguration configuration = this.npcManagement.getInventoryConfiguration();
    ItemLayoutHolder layouts = configuration.getHolder(service.getConfiguration().getGroups().toArray(new String[0]));
    // get the service state
    ServiceInfoState state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(service);
    ItemLayout layout;
    switch (state) {
      case EMPTY_ONLINE:
        layout = layouts.getEmptyLayout();
        break;
      case FULL_ONLINE:
        if (configuration.isShowFullServices()) {
          layout = layouts.getFullLayout();
          break;
        } else {
          return;
        }
      case ONLINE:
        layout = layouts.getOnlineLayout();
        break;
      default:
        return;
    }
    // build the item stack from the layout
    ItemStack item = this.buildItemStack(layout, service);
    if (item != null) {
      this.serviceItems.put(service, item);
      // push the service update
      this.rebuildInfoLines();
      this.rebuildInventory(configuration);
    }
  }

  @Override
  public void stopTrackingService(@NotNull ServiceInfoSnapshot service) {
    Bukkit.getScheduler().runTask(this.plugin, () -> {
      // try to find the old service
      for (ServiceInfoSnapshot snapshot : this.serviceItems.keySet()) {
        if (snapshot.getServiceId().getUniqueId().equals(service.getServiceId().getUniqueId())) {
          // remove the service
          this.serviceItems.remove(snapshot);
          // rebuild the inventory and info lines
          this.rebuildInfoLines();
          this.rebuildInventory(this.npcManagement.getInventoryConfiguration());
          // no need to dig further
          break;
        }
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
    for (Entry<ServiceInfoSnapshot, ItemStack> entry : this.serviceItems.entrySet()) {
      if (entry.getValue().equals(clickedItem)) {
        // close the inventory
        player.closeInventory();
        // connect the player
        this.getPlayerManager().getPlayerExecutor(player.getUniqueId()).connect(entry.getKey().getName());
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
    int chunkX = NumberConversions.floor(this.npcLocation.getX()) >> 4;
    int chunkZ = NumberConversions.floor(this.npcLocation.getZ()) >> 4;

    return this.npcLocation.getWorld() != null && this.npcLocation.getWorld().isChunkLoaded(chunkX, chunkZ);
  }

  protected void handleClickAction(@NotNull Player player, @NotNull ClickAction action) {
    switch (action) {
      case OPEN_INVENTORY:
        player.openInventory(this.inventory);
        break;
      case DIRECT_CONNECT_RANDOM: {
        List<ServiceInfoSnapshot> services = new ArrayList<>(this.serviceItems.keySet());
        // connect the player to the first element if present
        if (!services.isEmpty()) {
          ServiceInfoSnapshot service = services.get(ThreadLocalRandom.current().nextInt(0, services.size()));
          this.getPlayerManager().getPlayerExecutor(player.getUniqueId()).connect(service.getName());
        }
      }
      break;
      case DIRECT_CONNECT_LOWEST_PLAYERS: {
        this.serviceItems.keySet().stream()
          .min(Comparator.comparingInt(service -> BridgeServiceProperties.ONLINE_COUNT.get(service).orElse(0)))
          .ifPresent(ser -> this.getPlayerManager().getPlayerExecutor(player.getUniqueId()).connect(ser.getName()));
      }
      break;
      case DIRECT_CONNECT_HIGHEST_PLAYERS: {
        this.serviceItems.keySet().stream()
          .max(Comparator.comparingInt(service -> BridgeServiceProperties.ONLINE_COUNT.get(service).orElse(0)))
          .ifPresent(ser -> this.getPlayerManager().getPlayerExecutor(player.getUniqueId()).connect(ser.getName()));
      }
      break;
      default:
      case NOTHING:
        break;
    }
  }

  protected @Nullable ItemStack buildItemStack(@NotNull ItemLayout layout, @Nullable ServiceInfoSnapshot service) {
    Material material = Material.matchMaterial(layout.getMaterial());
    if (material != null) {
      ItemStack item = layout.getSubId() == -1
        ? new ItemStack(material)
        : new ItemStack(material, 1, (byte) layout.getSubId());
      // apply the meta
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
        meta.setDisplayName(BridgeServiceHelper.fillCommonPlaceholders(
          layout.getDisplayName(),
          this.npc.getTargetGroup(),
          service));
        meta.setLore(layout.getLore().stream()
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
    int inventorySize = configuration.getInventorySize();
    if (configuration.isDynamicSize()) {
      inventorySize = this.serviceItems.size();
      // try to make it to the next higher possible inventory size
      while (inventorySize == 0 || (inventorySize < 54 && inventorySize % 9 != 0)) {
        inventorySize++;
      }
    }
    // create the inventory
    Inventory inventory = this.inventory;
    if (inventory == null || inventory.getSize() != inventorySize) {
      this.inventory = inventory = Bukkit.createInventory(null, inventorySize, this.npc.getDisplayName());
    }
    // remove all current contents
    inventory.clear();
    // add the fixed items
    for (Entry<Integer, ItemLayout> entry : configuration.getFixedItems().entrySet()) {
      // check if the item would exceed the inventory size
      if (entry.getKey() < inventorySize) {
        // build and set the item
        ItemStack item = this.buildItemStack(entry.getValue(), null);
        if (item != null) {
          inventory.setItem(entry.getKey(), item);
        }
      }
    }
    // add the service items
    for (ItemStack value : this.serviceItems.values()) {
      if (!inventory.addItem(value).isEmpty()) {
        // the inventory is full
        break;
      }
    }
  }

  protected @NotNull IPlayerManager getPlayerManager() {
    return CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);
  }

  protected double getHeightAddition(int lineNumber) {
    NPCConfigurationEntry entry = this.npcManagement.getApplicableNPCConfigurationEntry();
    return entry == null ? lineNumber : entry.getInfoLineDistance() * lineNumber;
  }

  protected abstract void spawn0();

  protected abstract void remove0();

  protected abstract void addGlowingEffect();

  protected final class InfoLineWrapper {

    private final String basedInfoLine;
    private final ArmorStand armorStand;

    public InfoLineWrapper(String basedInfoLine, ArmorStand armorStand) {
      this.basedInfoLine = basedInfoLine;
      this.armorStand = armorStand;
    }

    private void rebuildInfoLine() {
      NPC npc = BukkitPlatformSelectorEntity.this.npc;
      // update based on the tracked services
      Set<ServiceInfoSnapshot> tracked = BukkitPlatformSelectorEntity.this.serviceItems.keySet();
      // general info
      String onlinePlayers = Integer.toString(tracked.stream()
        .mapToInt(snapshot -> BridgeServiceProperties.ONLINE_COUNT.get(snapshot).orElse(0))
        .sum());
      String maxPlayers = Integer.toString(tracked.stream()
        .mapToInt(snapshot -> BridgeServiceProperties.MAX_PLAYERS.get(snapshot).orElse(0))
        .sum());
      String onlineServers = Integer.toString(tracked.size());
      // rebuild the info line
      String newInfoLine = this.basedInfoLine
        .replace("%group%", npc.getTargetGroup()).replace("%g%", npc.getTargetGroup())
        .replace("%online_players%", onlinePlayers).replace("%o_p%", onlinePlayers)
        .replace("%max_players%", maxPlayers).replace("%m_p%", maxPlayers)
        .replace("%online_servers%", onlineServers).replace("%o_s%", onlineServers);
      // set the custom name of the armor stand
      this.armorStand.setCustomName(newInfoLine);
    }
  }
}
