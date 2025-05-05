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

package eu.cloudnetservice.modules.npc.impl.platform.bukkit.entity;

import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.configuration.InventoryConfiguration;
import eu.cloudnetservice.modules.npc.configuration.ItemLayout;
import eu.cloudnetservice.modules.npc.impl.platform.bukkit.BukkitPlatformNPCManagement;
import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.Nullable;

public abstract class BukkitPlatformSelectorEntity
  implements PlatformSelectorEntity<Location, Player, ItemStack, Inventory, Scoreboard> {

  protected static final MethodAccessor<?> SET_COLOR = Reflexion.on(Team.class)
    .findMethod("setColor", ChatColor.class)
    .orElse(null);

  protected static final int MAX_INVENTORY_ROWS = 6;
  protected static final int MAX_INVENTORY_ROW_ITEMS = 9;
  protected static final int MAX_INVENTORY_SIZE = MAX_INVENTORY_ROW_ITEMS * MAX_INVENTORY_ROWS;

  protected final NPC npc;
  protected final Plugin plugin;
  protected final Server server;
  protected final BukkitScheduler scheduler;
  protected final PlayerManager playerManager;
  protected final BukkitPlatformNPCManagement npcManagement;

  protected final UUID uniqueId;
  protected final String scoreboardTeamName;

  protected final Set<Integer> infoLineEntityIds = new HashSet<>();
  protected final Set<InfoLineWrapper> infoLines = new HashSet<>();
  protected final Map<UUID, ServiceItemWrapper> serviceItems = new LinkedHashMap<>();

  protected volatile Inventory inventory;
  protected volatile Location npcLocation;

  protected BukkitPlatformSelectorEntity(
    @NonNull NPC npc,
    @NonNull Plugin plugin,
    @NonNull Server server,
    @NonNull BukkitScheduler scheduler,
    @NonNull PlayerManager playerManager,
    @NonNull BukkitPlatformNPCManagement npcManagement
  ) {
    this.npc = npc;
    this.plugin = plugin;
    this.server = server;
    this.scheduler = scheduler;
    this.playerManager = playerManager;
    this.npcManagement = npcManagement;
    this.npcLocation = npcManagement.toPlatformLocation(npc.location());
    // construct the unique id randomly
    this.uniqueId = new UUID(ThreadLocalRandom.current().nextLong(), 0);
    this.scoreboardTeamName = this.uniqueId.toString().substring(0, 16);
  }

  @Override
  public void spawn() {
    this.scheduler.runTask(this.plugin, () -> {
      // create the inventory view
      this.rebuildInventory(this.npcManagement.inventoryConfiguration());
      // spawn the selector entity
      this.spawn0();
      // do the scoreboard related stuff now
      for (var player : this.server.getOnlinePlayers()) {
        this.registerScoreboardTeam(player.getScoreboard());
      }
      // let the entity glow!
      if (this.npc.glowing()) {
        this.addGlowingEffect();
      }
      // spawn the info lines
      for (var i = this.npc.infoLines().size() - 1; i >= 0; i--) {
        var armorStand = (ArmorStand) this.npcLocation.getWorld().spawnEntity(
          this.npcLocation.clone().add(0, this.heightAddition(i), 0),
          EntityType.ARMOR_STAND);
        armorStand.setSmall(true);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setBasePlate(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomNameVisible(true);
        // if it is the top info line try to spawn the item above it
        if (i == this.npc.infoLines().size() - 1) {
          var materialName = this.npc.floatingItem();
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
        var wrapper = new InfoLineWrapper(this.npc.infoLines().get(i), armorStand);
        wrapper.rebuildInfoLine();
        // register the line
        this.infoLines.add(wrapper);
        this.infoLineEntityIds.add(armorStand.getEntityId());
      }
    });
  }

  @Override
  public void remove() {
    // remove instantly when on main thread, else delay by one tick because we need to be on the main thread
    if (this.server.isPrimaryThread()) {
      this.doRemove();
    } else {
      this.scheduler.runTask(this.plugin, this::doRemove);
    }
  }

  protected void doRemove() {
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
  }

  @Override
  public void update() {
    // rebuild all items - we can do that async
    this.serviceItems.values().forEach(wrapper -> this.trackService(wrapper.service()));
    // rebuild everything else sync
    this.scheduler.runTask(this.plugin, () -> {
      this.rebuildInventory(this.npcManagement.inventoryConfiguration());
      this.rebuildInfoLines();
    });
  }

  @Override
  public void trackService(@NonNull ServiceInfoSnapshot service) {
    // get the current item
    var wrapper = this.serviceItems.get(service.serviceId().uniqueId());
    // build the item for the service
    var configuration = this.npcManagement.inventoryConfiguration();
    var layouts = configuration.getHolder(service.configuration().groups().toArray(new String[0]));
    // get the service state
    var state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(service);
    ItemLayout layout;
    switch (state) {
      case EMPTY_ONLINE -> layout = layouts.emptyLayout();
      case FULL_ONLINE -> {
        if (this.npc.showFullServices()) {
          layout = layouts.fullLayout();
        } else {
          this.unregisterItem(wrapper, service, configuration);
          return;
        }
      }
      case ONLINE -> layout = layouts.onlineLayout();
      case STOPPED -> {
        if (BridgeServiceHelper.inGameService(service) && this.npc.showIngameServices()) {
          layout = layouts.ingameLayout();
        } else {
          this.unregisterItem(wrapper, service, configuration);
          return;
        }
      }
      default -> {
        return;
      }
    }
    // build the item stack from the layout
    var item = this.buildItemStack(layout, service);
    if (item != null) {
      if (wrapper == null) {
        // store a new wrapper
        this.serviceItems.put(service.serviceId().uniqueId(), new ServiceItemWrapper(item, service));
      } else {
        // update the item wrapper
        wrapper.itemStack(item);
        wrapper.service(service);
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
  public void stopTrackingService(@NonNull ServiceInfoSnapshot service) {
    this.scheduler.runTask(this.plugin, () -> {
      // get the old item wrapper
      var wrapper = this.serviceItems.remove(service.serviceId().uniqueId());
      if (wrapper != null) {
        // the service got tracked before - rebuild the inventory and info lines
        this.rebuildInfoLines();
        this.rebuildInventory(this.npcManagement.inventoryConfiguration());
      }
    });
  }

  @Override
  public void handleLeftClickAction(@NonNull Player player) {
    this.handleClickAction(player, this.npc.leftClickAction());
  }

  @Override
  public void executeAction(@NonNull Player player, @NonNull NPC.ClickAction action) {
    this.handleClickAction(player, action);
  }

  @Override
  public void handleRightClickAction(@NonNull Player player) {
    this.handleClickAction(player, this.npc.rightClickAction());
  }

  @Override
  public void handleInventoryInteract(@NonNull Inventory inv, @NonNull Player player, @NonNull ItemStack clickedItem) {
    // find the server associated with the clicked item
    for (var wrapper : this.serviceItems.values()) {
      if (clickedItem.equals(wrapper.itemStack())) {
        // close the inventory
        player.closeInventory();
        // connect the player
        this.playerManager().playerExecutor(player.getUniqueId()).connect(wrapper.service().name());
        break;
      }
    }
  }

  @Override
  public void registerScoreboardTeam(@NonNull Scoreboard scoreboard) {
    // check if a team for this entity is already created
    var team = scoreboard.getTeam(this.scoreboardTeamName);
    if (team == null) {
      team = scoreboard.registerNewTeam(this.scoreboardTeamName);
    }
    // set the name tag visibility of the team
    team.setNameTagVisibility(NameTagVisibility.NEVER);
    // register the spawned entity to the team
    team.addEntry(this.scoreboardRepresentation());
    // check if the entity should have a glowing color
    if (SET_COLOR != null && this.npc.glowing()) {
      // try to set the team color
      var color = ChatColor.getByChar(this.npc.glowingColor());
      if (color != null) {
        SET_COLOR.invoke(team, color);
      }
    }
  }

  @Override
  public @NonNull Inventory selectorInventory() {
    return this.inventory;
  }

  @Override
  public @NonNull Set<Integer> infoLineEntityIds() {
    return this.infoLineEntityIds;
  }

  @Override
  public @NonNull NPC npc() {
    return this.npc;
  }

  @Override
  public @NonNull Location location() {
    var currentLoc = this.npcLocation;

    // check if the world in the location object & the world given by bukkit still match
    var world = this.server.getWorld(this.npc.location().world());
    if ((world != null && currentLoc.getWorld() != null) || (world == null && currentLoc.getWorld() == null)) {
      // all still the same
      return currentLoc;
    }

    // check if the world was un-/loaded
    if ((world != null && currentLoc.getWorld() == null) || (world == null && currentLoc.getWorld() != null)) {
      // re-init the location with the new world in it
      this.npcLocation = this.npcManagement.toPlatformLocation(this.npc.location());
      return this.npcLocation;
    }

    return currentLoc;
  }

  @Override
  public boolean canSpawn() {
    // use this.location() as it tries to initialize the location again if the world is missing
    var location = this.location();

    // ensure that the associated world is loaded
    var world = location.getWorld();
    if (world == null) {
      return false;
    }

    // ensure that the chunk the npc is located in is loaded
    var chunkX = NumberConversions.floor(location.getX()) >> 4;
    var chunkZ = NumberConversions.floor(location.getZ()) >> 4;
    return world.isChunkLoaded(chunkX, chunkZ);
  }

  protected void handleClickAction(@NonNull Player player, @NonNull NPC.ClickAction action) {
    switch (action) {
      case OPEN_INVENTORY -> player.openInventory(this.inventory);
      case DIRECT_CONNECT_RANDOM -> {
        var wrappers = this.serviceItems.values().stream()
          // make sure that we are allowed to connect to the service
          .filter(ServiceItemWrapper::canConnectTo)
          .toList();
        // connect the player to the first element if present
        if (!wrappers.isEmpty()) {
          var wrapper = wrappers.get(ThreadLocalRandom.current().nextInt(0, wrappers.size()));
          this.playerManager().playerExecutor(player.getUniqueId()).connect(wrapper.service().name());
        }
      }
      case DIRECT_CONNECT_LOWEST_PLAYERS -> this.serviceItems.values().stream()
        .filter(ServiceItemWrapper::canConnectTo)
        .map(ServiceItemWrapper::service)
        .min(Comparator.comparingInt(service -> service.readProperty(BridgeDocProperties.ONLINE_COUNT)))
        .ifPresent(ser -> this.playerManager().playerExecutor(player.getUniqueId()).connect(ser.name()));
      case DIRECT_CONNECT_HIGHEST_PLAYERS -> this.serviceItems.values().stream()
        .filter(ServiceItemWrapper::canConnectTo)
        .map(ServiceItemWrapper::service)
        .max(Comparator.comparingInt(service -> service.readProperty(BridgeDocProperties.ONLINE_COUNT)))
        .ifPresent(ser -> this.playerManager().playerExecutor(player.getUniqueId()).connect(ser.name()));
      default -> {
      }
    }
  }

  protected @Nullable ItemStack buildItemStack(@NonNull ItemLayout layout, @Nullable ServiceInfoSnapshot service) {
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
          this.npc.targetGroup(),
          service));
        meta.setLore(layout.lore().stream()
          .map(line -> BridgeServiceHelper.fillCommonPlaceholders(line, this.npc.targetGroup(), service))
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

  protected void unregisterItem(
    @Nullable ServiceItemWrapper wrapper,
    @NonNull ServiceInfoSnapshot service,
    @NonNull InventoryConfiguration configuration
  ) {
    if (wrapper != null) {
      // reset the ItemStack in the wrapper as we currently don't have an item to display
      wrapper.itemStack(null);
      // update the service and rebuild the inventory & infoline
      wrapper.service(service);

      this.rebuildInfoLines();
      this.rebuildInventory(configuration);
    }
  }

  protected void rebuildInfoLines() {
    this.infoLines.forEach(InfoLineWrapper::rebuildInfoLine);
  }

  protected void rebuildInventory(@NonNull InventoryConfiguration configuration) {
    // calculate the inventory size
    var inventorySize = this.calculateInventorySize(configuration);
    // create the inventory
    var inventory = this.inventory;
    if (inventory == null || inventory.getSize() != inventorySize) {
      this.inventory = inventory = this.server.createInventory(
        null,
        inventorySize,
        Objects.requireNonNullElse(this.npc.inventoryName(), InventoryType.CHEST.getDefaultTitle()));
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
      if (wrapper.canConnectTo() && !inventory.addItem(wrapper.itemStack()).isEmpty()) {
        // the inventory is full
        break;
      }
    }
  }

  protected int calculateInventorySize(@NonNull InventoryConfiguration configuration) {
    var inventorySize = configuration.inventorySize();
    if (configuration.dynamicSize()) {
      // dynamic size: create the smallest possible inventory that can fit all items (one row can fit 9 items)
      inventorySize = this.serviceItems.size();
      inventorySize += MAX_INVENTORY_ROW_ITEMS - (inventorySize % MAX_INVENTORY_ROW_ITEMS);
    }

    // minecraft inventories have a limit of 54 items
    return Math.min(MAX_INVENTORY_SIZE, inventorySize);
  }

  protected @NonNull PlayerManager playerManager() {
    return this.playerManager;
  }

  protected double heightAddition(int lineNumber) {
    var entry = this.npcManagement.applicableNPCConfigurationEntry();
    return entry == null ? lineNumber : entry.infoLineDistance() * lineNumber;
  }

  protected abstract void spawn0();

  protected abstract void remove0();

  protected abstract void addGlowingEffect();

  protected static final class ServiceItemWrapper {

    private volatile ItemStack itemStack;
    private volatile ServiceInfoSnapshot serviceInfoSnapshot;

    public ServiceItemWrapper(@Nullable ItemStack itemStack, @NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
      this.itemStack = itemStack;
      this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public @Nullable ItemStack itemStack() {
      return this.itemStack;
    }

    public void itemStack(@Nullable ItemStack itemStack) {
      this.itemStack = itemStack;
    }

    public @NonNull ServiceInfoSnapshot service() {
      return this.serviceInfoSnapshot;
    }

    public void service(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
      this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public boolean canConnectTo() {
      return this.itemStack != null;
    }
  }

  protected final class InfoLineWrapper {

    private final String basedInfoLine;
    private final ArmorStand armorStand;

    public InfoLineWrapper(@NonNull String basedInfoLine, @NonNull ArmorStand armorStand) {
      this.basedInfoLine = basedInfoLine;
      this.armorStand = armorStand;
    }

    private void rebuildInfoLine() {
      var npc = BukkitPlatformSelectorEntity.this.npc;
      // update based on the tracked services
      var tracked = BukkitPlatformSelectorEntity.this.serviceItems.values();
      // general info
      var onlinePlayers = Integer.toString(tracked.stream()
        .map(ServiceItemWrapper::service)
        .mapToInt(snapshot -> snapshot.readProperty(BridgeDocProperties.ONLINE_COUNT))
        .sum());
      var maxPlayers = Integer.toString(tracked.stream()
        .map(ServiceItemWrapper::service)
        .mapToInt(snapshot -> snapshot.readProperty(BridgeDocProperties.MAX_PLAYERS))
        .sum());
      var onlineServers = Integer.toString(tracked.size());
      // rebuild the info line
      var newInfoLine = this.basedInfoLine
        .replace("%group%", npc.targetGroup()).replace("%g%", npc.targetGroup())
        .replace("%online_players%", onlinePlayers).replace("%o_p%", onlinePlayers)
        .replace("%max_players%", maxPlayers).replace("%m_p%", maxPlayers)
        .replace("%online_servers%", onlineServers).replace("%o_s%", onlineServers);
      // set the custom name of the armor stand
      this.armorStand.setCustomName(ComponentFormats.ADVENTURE_TO_BUNGEE.convertText(newInfoLine));
    }
  }
}
