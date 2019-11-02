package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import org.bukkit.event.HandlerList;

public final class BukkitCloudNetTickEvent extends BukkitCloudNetEvent {

    private static HandlerList handlerList = new HandlerList();

    public BukkitCloudNetTickEvent() {
    }

    public static HandlerList getHandlerList() {
        return BukkitCloudNetTickEvent.handlerList;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}