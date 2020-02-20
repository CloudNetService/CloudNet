package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;


import com.comphenix.protocol.wrappers.EnumWrappers;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.realpanamo.npc.NPC;
import com.github.realpanamo.npc.NPCPool;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.npcs.AbstractNPCManagement;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class BukkitNPCManagement extends AbstractNPCManagement {

    private NPCPool npcPool;

    private ItemStack[] defaultItems;

    private Map<CloudNPC, Inventory> npcInventories = new HashMap<>();

    public BukkitNPCManagement(@NotNull NPCPool npcPool) {
        this.npcPool = npcPool;

        super.cloudNPCS.forEach(this::createNPC);
    }

    @Override
    protected void handleUpdate() {
        this.npcInventories.forEach(this::updateInventory);
    }

    @Override
    public boolean addNPC(@NotNull CloudNPC npc) {
        boolean success = super.addNPC(npc);

        if (success) {
            this.createNPC(npc);
        }

        return success;
    }

    @Override
    public void removeNPC(@NotNull CloudNPC npc) {
        this.npcInventories.remove(npc);
        this.npcPool.removeNPC(npc.getEntityId());

        super.removeNPC(npc);
    }

    @Override
    public void setNPCConfiguration(NPCConfiguration npcConfiguration) {
        super.setNPCConfiguration(npcConfiguration);

        this.createDefaultItems();
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

    private Inventory createInventory(CloudNPC cloudNPC) {
        Inventory inventory = Bukkit.createInventory(null, this.defaultItems.length, cloudNPC.getDisplayName());

        this.npcInventories.put(cloudNPC, inventory);

        return inventory;
    }

    public Inventory getInventory(CloudNPC cloudNPC) {
        return this.npcInventories.get(cloudNPC);
    }

    @Nullable
    public CloudNPC getByInventory(Inventory inventory) {
        return this.npcInventories.entrySet().stream()
                .filter(entry -> entry.getValue().equals(inventory))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void updateInventory(@NotNull CloudNPC cloudNPC, @NotNull Inventory inventory) {
        cloudNPC.getServerSlots().clear();

        List<Pair<ServiceInfoSnapshot, ServiceInfoState>> services = super.services.values().stream()
                .filter(pair -> (pair.getSecond() != ServiceInfoState.STOPPED && pair.getSecond() != ServiceInfoState.STARTING)
                        && Arrays.asList(pair.getFirst().getConfiguration().getGroups()).contains(cloudNPC.getTargetGroup()))
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
            items[slot] = this.toItemStack(itemLayout, cloudNPC.getTargetGroup(), infoSnapshot);

            cloudNPC.getServerSlots().put(slot, infoSnapshot.getName());
        }

        inventory.setContents(items);
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
        this.updateInventory(cloudNPC, inventory);
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
