package eu.cloudnetservice.cloudnet.ext.npcs.bukkit;


import org.bukkit.inventory.Inventory;

import java.util.Map;

public class BukkitNPCProperties {

    private int entityId;

    private Inventory inventory;

    private Map<Integer, String> serverSlots;

    public BukkitNPCProperties(int entityId, Inventory inventory, Map<Integer, String> serverSlots) {
        this.entityId = entityId;
        this.inventory = inventory;
        this.serverSlots = serverSlots;
    }

    public int getEntityId() {
        return entityId;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Map<Integer, String> getServerSlots() {
        return serverSlots;
    }

}
