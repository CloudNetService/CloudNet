package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;


import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public class BukkitNPCProperties {

    private final CloudNPC holder;

    private final int entityId;

    private final Inventory inventory;

    private final Map<Integer, String> serverSlots;

    public BukkitNPCProperties(CloudNPC holder, int entityId, Inventory inventory, Map<Integer, String> serverSlots) {
        this.holder = holder;
        this.entityId = entityId;
        this.inventory = inventory;
        this.serverSlots = serverSlots;
    }

    public CloudNPC getHolder() {
        return this.holder;
    }

    public int getEntityId() {
        return this.entityId;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public Map<Integer, String> getServerSlots() {
        return this.serverSlots;
    }

}
