package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;

public final class NukkitCloudNetTickEvent extends NukkitCloudNetEvent {

    private static final HandlerList handlers = new HandlerList();

    public NukkitCloudNetTickEvent() {
    }

    public static HandlerList getHandlers() {
        return NukkitCloudNetTickEvent.handlers;
    }
}