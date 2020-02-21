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
import org.bukkit.entity.Entity;
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

    private static final String NPC_ID_METADATA = "npcEntityId";

    private JavaPlugin javaPlugin;

    private NPCPool npcPool;

    private ItemStack[] defaultItems;

    private Map<CloudNPC, Inventory> npcInventories = new HashMap<>();

    public BukkitNPCManagement(@NotNull JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
        this.npcPool = new NPCPool(javaPlugin);

        super.cloudNPCS.forEach(this::createNPC);
    }

    @Override
    protected void handleUpdate() {
        Bukkit.getScheduler().runTask(this.javaPlugin, () -> this.npcInventories.forEach(this::updateNPC));
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

        this.createDefaultItems();
    }

    public void shutdown() {
        super.cloudNPCS.forEach(this::destroyNPC);
    }

    private void createDefaultItems() {
        int inventorySize = super.ownNPCConfigurationEntry.getInventorySize();
        this.defaultItems = new ItemStack[inventorySize % 9 == 0 ? inventorySize : 54];

        Map<Integer, NPCConfigurationEntry.ItemLayout> inventoryLayout = super.ownNPCConfigurationEntry.getInventoryLayout();
        for (int index = 0; index < this.defaultItems.length; index++) {
            if (inventoryLayout.containsKey(index)) {
                this.defaultItems[index] = this.toItemStack(inventoryLayout.get(index));
            }
        }
    }

    public Inventory getInventory(CloudNPC cloudNPC) {
        return this.npcInventories.get(cloudNPC);
    }

    public Optional<ArmorStand> getInfoLineStand(CloudNPC cloudNPC) {
        Location location = this.toLocation(cloudNPC.getPosition());

        if (location != null) {
            return location.getWorld()
                    .getNearbyEntitiesByType(ArmorStand.class, location, super.ownNPCConfigurationEntry.getInfoLineDistance() + 0.1D)
                    .stream()
                    .findFirst();
        }

        return Optional.empty();
    }

    private Inventory createInventory(CloudNPC cloudNPC) {
        Inventory inventory = Bukkit.createInventory(null, this.defaultItems.length, cloudNPC.getDisplayName());

        this.npcInventories.put(cloudNPC, inventory);

        return inventory;
    }

    public void updateNPC(@NotNull CloudNPC cloudNPC, @NotNull Inventory inventory) {
        String targetGroup = cloudNPC.getTargetGroup();

        List<Pair<ServiceInfoSnapshot, ServiceInfoState>> services = super.services.values().stream()
                .filter(pair -> (pair.getSecond() != ServiceInfoState.STOPPED && pair.getSecond() != ServiceInfoState.STARTING)
                        && Arrays.asList(pair.getFirst().getConfiguration().getGroups()).contains(targetGroup))
                .sorted(Comparator.comparingInt(pair -> pair.getFirst().getServiceId().getTaskServiceId()))
                .collect(Collectors.toList());

        ItemStack[] items = this.defaultItems.clone();

        for (int index = 0; index < services.size(); index++) {
            Pair<ServiceInfoSnapshot, ServiceInfoState> serviceInfo = services.get(index);

            ServiceInfoSnapshot infoSnapshot = serviceInfo.getFirst();
            ServiceInfoState infoState = serviceInfo.getSecond();

            NPCConfigurationEntry.ItemLayout itemLayout = infoState == ServiceInfoState.FULL_ONLINE
                    ? super.ownNPCConfigurationEntry.getFullItem()
                    : infoState == ServiceInfoState.EMPTY_ONLINE ? super.ownNPCConfigurationEntry.getEmptyItem()
                    : super.ownNPCConfigurationEntry.getOnlineItem();

            int slot = index + super.ownNPCConfigurationEntry.getStartSlot();

            ItemStack itemStack = this.toItemStack(itemLayout, cloudNPC.getTargetGroup(), infoSnapshot);
            if (itemStack != null) {
                this.writeServer(itemStack, slot, infoSnapshot.getName());
            }

            items[slot] = itemStack;
        }

        inventory.setContents(items);

        this.getInfoLineStand(cloudNPC).ifPresent(armorStand -> {
            String infoLine = cloudNPC.getInfoLine()
                    .replace("%group%", String.valueOf(targetGroup))
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

            armorStand.setCustomName(ChatColor.translateAlternateColorCodes('&', infoLine));
        });

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
                        .map(npcProfileProperty -> new ProfileProperty(npcProfileProperty.getName(), npcProfileProperty.getValue(), npcProfileProperty.getSignature()))
                        .collect(Collectors.toSet()),
                cloudNPC.getDisplayName()
        );

        Location location = this.toLocation(cloudNPC.getPosition());

        if (location != null) {
            builder.location(location);

            this.createArmorStand(location.clone().add(0D, super.ownNPCConfigurationEntry.getInfoLineDistance(), 0));
        }

        NPC npc = builder.uuid(cloudNPC.getUUID())
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

        Inventory inventory = this.createInventory(cloudNPC);
        this.updateNPC(cloudNPC, inventory);
    }

    private void createArmorStand(Location location) {
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);

        armorStand.setVisible(false);
        armorStand.setAI(false);
        armorStand.setGravity(false);

        armorStand.setDisabledSlots(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);

        armorStand.setCustomNameVisible(true);
    }

    private void destroyNPC(CloudNPC cloudNPC) {
        this.npcInventories.remove(cloudNPC);
        this.npcPool.removeNPC(cloudNPC.getEntityId());

        this.getInfoLineStand(cloudNPC).ifPresent(Entity::remove);
    }

    private Location toLocation(WorldPosition position) {
        World world = Bukkit.getWorld(position.getWorld());

        return world == null ? null : new Location(world, position.getX(), position.getY(), position.getZ(), (float) position.getYaw(), (float) position.getPitch());
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
