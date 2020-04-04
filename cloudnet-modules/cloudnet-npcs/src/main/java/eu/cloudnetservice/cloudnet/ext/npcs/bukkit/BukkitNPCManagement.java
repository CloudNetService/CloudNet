package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;


import com.comphenix.protocol.wrappers.EnumWrappers;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.juliarn.npc.NPC;
import com.github.juliarn.npc.NPCPool;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoSnapshotUtil;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.npcs.AbstractNPCManagement;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class BukkitNPCManagement extends AbstractNPCManagement {

    private JavaPlugin javaPlugin;

    private NPCPool npcPool;

    private ItemStack[] defaultItems;

    private Map<UUID, BukkitNPCProperties> npcProperties = new HashMap<>();

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

        int inventorySize = super.ownNPCConfigurationEntry.getInventorySize();
        this.defaultItems = new ItemStack[inventorySize % 9 == 0 ? inventorySize : 54];

        Map<Integer, NPCConfigurationEntry.ItemLayout> inventoryLayout = super.ownNPCConfigurationEntry.getInventoryLayout();
        for (int index = 0; index < this.defaultItems.length; index++) {
            if (inventoryLayout.containsKey(index + 1)) {
                this.defaultItems[index] = this.toItemStack(inventoryLayout.get(index + 1));
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

    public Optional<ArmorStand> getInfoLineStand(@NotNull CloudNPC cloudNPC) {
        Location location = this.toLocation(cloudNPC.getPosition());

        if (location.getWorld() == null || !location.isChunkLoaded()) {
            return Optional.empty();
        }

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

        return Optional.of(armorStand);
    }

    public void updateNPC(@NotNull CloudNPC cloudNPC) {
        if (!this.npcProperties.containsKey(cloudNPC.getUUID())) {
            this.createNPC(cloudNPC);
            return;
        }

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

        BukkitNPCProperties properties = this.npcProperties.get(cloudNPC.getUUID());
        properties.getServerSlots().clear();

        for (int index = 0; index < services.size(); index++) {
            Pair<ServiceInfoSnapshot, ServiceInfoState> serviceInfo = services.get(index);

            NPCConfigurationEntry.ItemLayout itemLayout = this.itemLayouts.getOrDefault(serviceInfo.getSecond(), super.ownNPCConfigurationEntry.getOnlineItem());
            ServiceInfoSnapshot infoSnapshot = serviceInfo.getFirst();

            ItemStack itemStack = this.toItemStack(itemLayout, cloudNPC.getTargetGroup(), infoSnapshot);
            int slot = index + super.ownNPCConfigurationEntry.getStartSlot() - 1;

            if (slot < 0 || slot > super.ownNPCConfigurationEntry.getEndSlot() - 1) {
                break;
            }

            properties.getServerSlots().put(slot, infoSnapshot.getName());
            items[slot] = itemStack;
        }

        properties.getInventory().setContents(items);
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

        this.getInfoLineStand(cloudNPC).ifPresent(infoLineStand -> infoLineStand.setCustomName(infoLine));
    }

    private void createNPC(CloudNPC cloudNPC) {
        if (!this.isWorldLoaded(cloudNPC)) {
            return;
        }

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

        Location location = this.toLocation(cloudNPC.getPosition());

        NPC npc = builder
                .uuid(cloudNPC.getUUID())
                .location(location)
                .lookAtPlayer(cloudNPC.isLookAtPlayer())
                .imitatePlayer(cloudNPC.isImitatePlayer())
                .spawnCustomizer((spawnedNPC, player) -> {
                    spawnedNPC.rotation().queueRotate(location.getYaw(), location.getPitch()).send(player);
                    spawnedNPC.metadata()
                            .queueSkinLayers(true)
                            .queueSneaking(false)
                            .send(player);

                    Material material = Material.getMaterial(cloudNPC.getItemInHand());
                    if (material != null) {
                        spawnedNPC.equipment().queue(EnumWrappers.ItemSlot.MAINHAND, new ItemStack(material)).send(player);
                    }
                }).build(this.npcPool);

        this.npcProperties.put(cloudNPC.getUUID(), new BukkitNPCProperties(
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

    public Collection<BukkitNPCProperties> getNPCProperties() {
        return npcProperties.values();
    }

}
