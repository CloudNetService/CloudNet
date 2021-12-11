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

package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;

import com.github.juliarn.npc.NPC;
import com.github.juliarn.npc.NPCPool;
import com.github.juliarn.npc.modifier.AnimationModifier;
import com.github.juliarn.npc.modifier.EquipmentModifier;
import com.github.juliarn.npc.modifier.MetadataModifier;
import com.github.juliarn.npc.profile.Profile;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.npcs.AbstractNPCManagement;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class BukkitNPCManagement extends AbstractNPCManagement {

  private final JavaPlugin javaPlugin;

  private final NPCPool npcPool;
  private final Map<UUID, BukkitNPCProperties> npcProperties = new HashMap<>();
  private ItemStack[] defaultItems;

  public BukkitNPCManagement(@NotNull JavaPlugin javaPlugin) {
    this.javaPlugin = javaPlugin;
    this.npcPool = NPCPool.builder(javaPlugin)
      .tabListRemoveTicks(super.ownNPCConfigurationEntry.getNPCTabListRemoveTicks())
      .build();

    super.cloudNPCS.forEach(this::createNPC);
  }

  @Override
  protected void handleUpdate() {
    Bukkit.getScheduler().runTask(this.javaPlugin, () -> super.cloudNPCS.forEach(this::updateNPC));
  }

  @Override
  public boolean addNPC(@NotNull CloudNPC npc) {
    boolean allowed = super.addNPC(npc);

    if (allowed) {
      if (this.npcProperties.containsKey(npc.getUUID())) {
        Bukkit.getScheduler().runTask(this.javaPlugin, () -> this.destroyNPC(npc));
      }

      Bukkit.getScheduler().runTaskLater(this.javaPlugin, () -> this.createNPC(npc), 2L);
    }

    return allowed;
  }

  @Override
  public void removeNPC(@NotNull CloudNPC npc) {
    Bukkit.getScheduler().runTask(this.javaPlugin, () -> this.destroyNPC(npc));

    super.removeNPC(npc);
  }

  @Override
  public void setNPCConfiguration(NPCConfiguration npcConfiguration) {
    super.setNPCConfiguration(npcConfiguration);

    int inventorySize =
      super.ownNPCConfigurationEntry.getInventorySize() % 9 == 0 ? super.ownNPCConfigurationEntry.getInventorySize()
        : 54;
    this.defaultItems = new ItemStack[inventorySize];

    if (super.ownNPCConfigurationEntry.getEndSlot() > inventorySize) {
      super.ownNPCConfigurationEntry.setEndSlot(inventorySize);
    }

    Map<Integer, NPCConfigurationEntry.ItemLayout> inventoryLayout = super.ownNPCConfigurationEntry
      .getInventoryLayout();
    for (int index = 0; index < this.defaultItems.length; index++) {
      if (inventoryLayout.containsKey(index + 1)) {
        this.defaultItems[index] = this.toItemStack(inventoryLayout.get(index + 1));
      }
    }
  }

  public void shutdown() {
    super.cloudNPCS.forEach(cloudNPC -> this.getInfoLineStand(cloudNPC).ifPresent(Entity::remove));
  }

  public Optional<ArmorStand> getInfoLineStand(@NotNull CloudNPC cloudNPC) {
    return this.getInfoLineStand(cloudNPC, true);
  }

  public Optional<ArmorStand> getInfoLineStand(@NotNull CloudNPC cloudNPC, boolean doSpawnIfMissing) {
    Location location = this.toLocation(cloudNPC.getPosition());

    if (location.getWorld() == null || !location.getChunk().isLoaded()) {
      return Optional.empty();
    }

    double infoLineDistance = super.ownNPCConfigurationEntry.getInfoLineDistance();

    ArmorStand armorStand = (ArmorStand) location.getWorld()
      .getNearbyEntities(location, infoLineDistance + 0.1D, infoLineDistance + 0.1D, infoLineDistance + 0.1D)
      .stream()
      .filter(entity -> entity instanceof ArmorStand)
      .findFirst()
      .orElse(null);

    if (armorStand == null && doSpawnIfMissing) {
      armorStand = (ArmorStand) location.getWorld().spawnEntity(
        location.add(0, infoLineDistance, 0),
        EntityType.ARMOR_STAND
      );

      armorStand.setVisible(false);
      armorStand.setGravity(false);
      armorStand.setCanPickupItems(false);

      armorStand.setCustomNameVisible(true);
    }

    return Optional.ofNullable(armorStand);
  }

  @Override
  public void updateNPC(@NotNull CloudNPC cloudNPC) {
    if (!this.npcProperties.containsKey(cloudNPC.getUUID())) {
      this.createNPC(cloudNPC);
      return;
    }

    List<Pair<ServiceInfoSnapshot, ServiceInfoState>> services = super.filterNPCServices(cloudNPC);

    this.updateInventory(cloudNPC, services);
    this.updateInfoLine(cloudNPC, super.services.values().stream()
      .map(Pair::getFirst)
      .filter(serviceInfoSnapshot -> Arrays.asList(serviceInfoSnapshot.getConfiguration().getGroups())
        .contains(cloudNPC.getTargetGroup()))
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING)
      .collect(Collectors.toList())
    );
  }

  private void updateInventory(CloudNPC cloudNPC, List<Pair<ServiceInfoSnapshot, ServiceInfoState>> services) {
    ItemStack[] items = this.defaultItems.clone();

    BukkitNPCProperties properties = this.npcProperties.get(cloudNPC.getUUID());
    properties.getServerSlots().clear();

    serviceLoop:
    for (int index = 0; index < services.size(); index++) {
      Pair<ServiceInfoSnapshot, ServiceInfoState> serviceInfo = services.get(index);

      NPCConfigurationEntry.ItemLayout itemLayout = this.itemLayouts
        .getOrDefault(serviceInfo.getSecond(), super.ownNPCConfigurationEntry.getOnlineItem());
      ServiceInfoSnapshot infoSnapshot = serviceInfo.getFirst();

      ItemStack itemStack = this.toItemStack(itemLayout, cloudNPC.getTargetGroup(), infoSnapshot);
      int slot = index + Math.max(super.ownNPCConfigurationEntry.getStartSlot(), 1) - 2;

      do {
        slot++;

        if (slot > super.ownNPCConfigurationEntry.getEndSlot() - 1) {
          break serviceLoop;
        }
      } while (items[slot] != null);

      properties.getServerSlots().put(slot, infoSnapshot.getName());
      items[slot] = itemStack;
    }

    properties.getInventory().setContents(items);
  }

  private void updateInfoLine(CloudNPC cloudNPC, List<ServiceInfoSnapshot> services) {
    String onlinePlayers = String.valueOf(
      services.stream()
        .mapToInt(serviceInfoSnapshot -> serviceInfoSnapshot.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0))
        .sum()
    );

    String maxPlayers = String.valueOf(
      services.stream()
        .mapToInt(serviceInfoSnapshot -> serviceInfoSnapshot.getProperty(BridgeServiceProperty.MAX_PLAYERS).orElse(0))
        .sum()
    );

    String onlineServers = String.valueOf(services.size());

    String infoLine = cloudNPC.getInfoLine()
      .replace("%group%", cloudNPC.getTargetGroup()).replace("%g%", cloudNPC.getTargetGroup())
      .replace("%online_players%", onlinePlayers).replace("%o_p%", onlinePlayers)
      .replace("%max_players%", maxPlayers).replace("%m_p%", maxPlayers)
      .replace("%online_servers%", onlineServers).replace("%o_s%", onlineServers);

    this.getInfoLineStand(cloudNPC).ifPresent(infoLineStand -> infoLineStand.setCustomName(infoLine));
  }

  private void createNPC(CloudNPC cloudNPC) {
    if (!this.isWorldLoaded(cloudNPC)) {
      return;
    }

    Location location = this.toLocation(cloudNPC.getPosition());

    NPC npc = NPC.builder()
      .profile(new Profile(
        cloudNPC.getUUID(),
        cloudNPC.getDisplayName(),
        cloudNPC.getProfileProperties().stream()
          .map(npcProfileProperty -> new Profile.Property(
            npcProfileProperty.getName(),
            npcProfileProperty.getValue(),
            npcProfileProperty.getSignature())
          )
          .collect(Collectors.toSet())
      ))
      .location(location)
      .lookAtPlayer(cloudNPC.isLookAtPlayer())
      .imitatePlayer(cloudNPC.isImitatePlayer())
      .spawnCustomizer((spawnedNPC, player) -> {
        spawnedNPC.rotation().queueRotate(location.getYaw(), location.getPitch()).send(player);
        spawnedNPC.metadata()
          .queue(MetadataModifier.EntityMetadata.SKIN_LAYERS, true)
          .queue(MetadataModifier.EntityMetadata.SNEAKING, false)
          .send(player);

        spawnedNPC.animation().queue(AnimationModifier.EntityAnimation.SWING_MAIN_ARM).send(player);

        Material material = Material.getMaterial(cloudNPC.getItemInHand());
        if (material != null) {
          spawnedNPC.equipment().queue(EquipmentModifier.MAINHAND, new ItemStack(material)).send(player);
        }
      }).build(this.npcPool);

    this.npcProperties.put(cloudNPC.getUUID(), new BukkitNPCProperties(
      cloudNPC,
      npc.getEntityId(),
      Bukkit.createInventory(null, this.defaultItems.length, cloudNPC.getDisplayName()),
      new HashMap<>()
    ));

    this.updateNPC(cloudNPC);
  }

  private void destroyNPC(CloudNPC cloudNPC) {
    this.getInfoLineStand(cloudNPC).ifPresent(Entity::remove);

    BukkitNPCProperties properties = this.npcProperties.remove(cloudNPC.getUUID());

    if (properties != null) {
      this.npcPool.removeNPC(properties.getEntityId());
    }
  }

  @Override
  public boolean isWorldLoaded(CloudNPC cloudNPC) {
    return this.toLocation(cloudNPC.getPosition()).getWorld() != null;
  }

  public Location toLocation(WorldPosition position) {
    return new Location(
      Bukkit.getWorld(position.getWorld()),
      position.getX(),
      position.getY(),
      position.getZ(),
      (float) position.getYaw(),
      (float) position.getPitch()
    );
  }

  private ItemStack toItemStack(NPCConfigurationEntry.ItemLayout itemLayout, String group,
    ServiceInfoSnapshot serviceInfoSnapshot) {
    Material material = Material.getMaterial(itemLayout.getMaterial());

    if (material != null) {
      ItemStack itemStack = itemLayout.getSubId() == -1 ? new ItemStack(material)
        : new ItemStack(material, 1, (byte) itemLayout.getSubId());

      ItemMeta meta = itemStack.getItemMeta();

      if (meta != null) {
        meta.setDisplayName(super.replaceServiceInfo(itemLayout.getDisplayName(), group, serviceInfoSnapshot));
        meta.setLore(itemLayout.getLore().stream()
          .map(line -> super.replaceServiceInfo(line, group, serviceInfoSnapshot))
          .collect(Collectors.toList()));

        itemStack.setItemMeta(meta);
      }

      return itemStack;
    }

    return null;
  }

  private ItemStack toItemStack(NPCConfigurationEntry.ItemLayout itemLayout) {
    return this.toItemStack(itemLayout, null, null);
  }


  public NPCPool getNPCPool() {
    return this.npcPool;
  }

  public ItemStack[] getDefaultItems() {
    return this.defaultItems;
  }

  public Map<ServiceInfoState, NPCConfigurationEntry.ItemLayout> getItemLayouts() {
    return this.itemLayouts;
  }

  public Map<UUID, BukkitNPCProperties> getNPCProperties() {
    return this.npcProperties;
  }

}
