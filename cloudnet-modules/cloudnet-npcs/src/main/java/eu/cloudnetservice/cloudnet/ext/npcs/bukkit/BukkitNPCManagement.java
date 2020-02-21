package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;


import com.comphenix.protocol.wrappers.EnumWrappers;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.realpanamo.npc.NPC;
import com.github.realpanamo.npc.NPCPool;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoSnapshotUtil;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.npcs.AbstractNPCManagement;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class BukkitNPCManagement extends AbstractNPCManagement {

    private JavaPlugin javaPlugin;

    private NPCPool npcPool;

    private ItemStack[] defaultItems;

    private Map<CloudNPC, Inventory> npcInventories = new HashMap<>();

    private Map<ServiceInfoState, NPCConfigurationEntry.ItemLayout> itemLayouts = new HashMap<>();

    public BukkitNPCManagement(@NotNull JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
        this.npcPool = new NPCPool(javaPlugin);

        super.cloudNPCS.forEach(this::createNPC);
    }

    @Override
    protected void handleUpdate() {
        Bukkit.getScheduler().runTask(this.javaPlugin, () -> super.cloudNPCS.forEach(this::updateNPC));
    }

    @Override
    public boolean addNPC(@NotNull CloudNPC npc) {
        boolean success = super.addNPC(npc);

        if (success) {
            Bukkit.getScheduler().runTask(this.javaPlugin, () -> this.createNPC(npc));
        }

        return success;
    }

    @Override
    public void removeNPC(@NotNull CloudNPC npc) {
        Bukkit.getScheduler().runTask(this.javaPlugin, () -> this.destroyNPC(npc));

        super.removeNPC(npc);
    }

    @Override
    public void setNPCConfiguration(NPCConfiguration npcConfiguration) {
        super.setNPCConfiguration(npcConfiguration);

        int inventorySize = super.ownNPCConfigurationEntry.getInventorySize();
        this.defaultItems = new ItemStack[inventorySize % 9 == 0 ? inventorySize : 54];

        Map<Integer, NPCConfigurationEntry.ItemLayout> inventoryLayout = super.ownNPCConfigurationEntry.getInventoryLayout();
        for (int index = 0; index < this.defaultItems.length; index++) {
            if (inventoryLayout.containsKey(index)) {
                this.defaultItems[index] = this.toItemStack(inventoryLayout.get(index));
            }
        }

        this.itemLayouts = new HashMap<>();

        this.itemLayouts.put(ServiceInfoState.ONLINE, super.ownNPCConfigurationEntry.getOnlineItem());
        this.itemLayouts.put(ServiceInfoState.EMPTY_ONLINE, super.ownNPCConfigurationEntry.getEmptyItem());
        this.itemLayouts.put(ServiceInfoState.FULL_ONLINE, super.ownNPCConfigurationEntry.getFullItem());
    }

    public void shutdown() {
        super.cloudNPCS.forEach(this::destroyNPC);
    }

    @NotNull
    public Inventory getInventory(@NotNull CloudNPC cloudNPC) {
        return this.npcInventories.computeIfAbsent(cloudNPC, npc ->
                Bukkit.createInventory(null, this.defaultItems.length, npc.getDisplayName()));
    }

    @NotNull
    public ArmorStand getInfoLineStand(@NotNull CloudNPC cloudNPC) {
        Location location = this.toLocation(cloudNPC.getPosition());

        ArmorStand armorStand = location.getWorld()
                .getNearbyEntitiesByType(ArmorStand.class, location, super.ownNPCConfigurationEntry.getInfoLineDistance() + 0.1D)
                .stream()
                .findFirst()
                .orElse(null);

        if (armorStand == null) {
            armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);

            armorStand.setVisible(false);
            armorStand.setAI(false);
            armorStand.setGravity(false);

            armorStand.setCanPickupItems(false);
            armorStand.setDisabledSlots(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);

            armorStand.setCustomNameVisible(true);
        }

        return armorStand;
    }

    public void updateNPC(@NotNull CloudNPC cloudNPC) {
        List<Pair<ServiceInfoSnapshot, ServiceInfoState>> services = this.filterNPCServices(cloudNPC);

        this.updateInventory(cloudNPC, services);
        this.updateInfoLine(cloudNPC, services);
    }

    public List<Pair<ServiceInfoSnapshot, ServiceInfoState>> filterNPCServices(@NotNull CloudNPC cloudNPC) {
        return super.services.values().stream()
                .filter(pair -> (pair.getSecond() != ServiceInfoState.STOPPED && pair.getSecond() != ServiceInfoState.STARTING)
                        && Arrays.asList(pair.getFirst().getConfiguration().getGroups()).contains(cloudNPC.getTargetGroup()))
                .sorted(Comparator.comparingInt(pair -> pair.getFirst().getServiceId().getTaskServiceId()))
                .collect(Collectors.toList());
    }

    private void updateInventory(CloudNPC cloudNPC, List<Pair<ServiceInfoSnapshot, ServiceInfoState>> services) {
        ItemStack[] items = this.defaultItems.clone();

        for (int index = 0; index < services.size(); index++) {
            Pair<ServiceInfoSnapshot, ServiceInfoState> serviceInfo = services.get(index);

            NPCConfigurationEntry.ItemLayout itemLayout = this.itemLayouts.getOrDefault(serviceInfo.getSecond(), super.ownNPCConfigurationEntry.getOnlineItem());
            ServiceInfoSnapshot infoSnapshot = serviceInfo.getFirst();

            ItemStack itemStack = this.toItemStack(itemLayout, cloudNPC.getTargetGroup(), infoSnapshot);
            int slot = index + super.ownNPCConfigurationEntry.getStartSlot();

            if (itemStack != null) {
                this.writeServer(itemStack, slot, infoSnapshot.getName());
            }

            items[slot] = itemStack;
        }

        this.getInventory(cloudNPC).setContents(items);
    }

    private void updateInfoLine(CloudNPC cloudNPC, List<Pair<ServiceInfoSnapshot, ServiceInfoState>> services) {
        String infoLine = cloudNPC.getInfoLine()
                .replace("%group%", String.valueOf(cloudNPC.getTargetGroup()))
                .replace("%online_players%", String.valueOf(
                        services.stream()
                                .mapToInt(pair -> ServiceInfoSnapshotUtil.getOnlineCount(pair.getFirst()))
                                .sum())
                )
                .replace("%max_players%", String.valueOf(
                        services.stream()
                                .mapToInt(pair -> ServiceInfoSnapshotUtil.getMaxPlayers(pair.getFirst()))
                                .sum())
                )
                .replace("%online_servers%", String.valueOf(services.size()));

        this.getInfoLineStand(cloudNPC).setCustomName(ChatColor.translateAlternateColorCodes('&', infoLine));
    }

    public void writeServer(@NotNull ItemStack itemStack, int slot, String serverName) {
        itemStack.getItemMeta().getPersistentDataContainer().set(
                new NamespacedKey(this.javaPlugin, String.valueOf(slot)),
                PersistentDataType.STRING,
                serverName
        );
    }

    @Nullable
    public String readServer(@NotNull ItemStack itemStack, int slot) {
        if (itemStack.hasItemMeta()) {
            return itemStack.getItemMeta().getPersistentDataContainer().get(
                    new NamespacedKey(this.javaPlugin, String.valueOf(slot)),
                    PersistentDataType.STRING
            );
        }

        return null;
    }

    private void createNPC(CloudNPC cloudNPC) {
        NPC.Builder builder = new NPC.Builder(
                cloudNPC.getProfileProperties().stream()
                        .map(npcProfileProperty -> new ProfileProperty(
                                npcProfileProperty.getName(),
                                npcProfileProperty.getValue(),
                                npcProfileProperty.getSignature())
                        )
                        .collect(Collectors.toSet()),
                cloudNPC.getDisplayName()
        );

        NPC npc = builder
                .uuid(cloudNPC.getUUID())
                .location(this.toLocation(cloudNPC.getPosition()))
                .lookAtPlayer(cloudNPC.isLookAtPlayer())
                .imitatePlayer(cloudNPC.isImitatePlayer())
                .spawnCustomizer((spawnedNPC, player) -> {
                    spawnedNPC.metadata().queueSkinLayers(true).send(player);

                    Material material = Material.getMaterial(cloudNPC.getItemInHand());
                    if (material != null) {
                        spawnedNPC.equipment().queue(EnumWrappers.ItemSlot.MAINHAND, new ItemStack(material)).send(player);
                    }
                }).build(this.npcPool);

        cloudNPC.setEntityId(npc.getEntityId());
        this.updateNPC(cloudNPC);
    }

    private void destroyNPC(CloudNPC cloudNPC) {
        this.npcInventories.remove(cloudNPC);
        this.getInfoLineStand(cloudNPC).remove();

        this.npcPool.removeNPC(cloudNPC.getEntityId());
    }

    private Location toLocation(WorldPosition position) {
        return new Location(
                Bukkit.getWorld(position.getWorld()),
                position.getX(),
                position.getY(),
                position.getZ(),
                (float) position.getYaw(),
                (float) position.getPitch()
        );
    }

    private ItemStack toItemStack(NPCConfigurationEntry.ItemLayout itemLayout, String group, ServiceInfoSnapshot serviceInfoSnapshot) {
        Material material = Material.getMaterial(itemLayout.getMaterial());

        if (material != null) {
            ItemStack itemStack = new ItemStack(material);

            ItemMeta meta = itemStack.getItemMeta();

            meta.setDisplayName(super.replaceServiceInfo(itemLayout.getDisplayName(), group, serviceInfoSnapshot));
            meta.setLore(itemLayout.getLore().stream()
                    .map(line -> super.replaceServiceInfo(line, group, serviceInfoSnapshot))
                    .collect(Collectors.toList()));

            itemStack.setItemMeta(meta);

            return itemStack;
        }

        return null;
    }

    private ItemStack toItemStack(NPCConfigurationEntry.ItemLayout itemLayout) {
        return this.toItemStack(itemLayout, null, null);
    }

    public Map<CloudNPC, Inventory> getNPCInventories() {
        return npcInventories;
    }

}
