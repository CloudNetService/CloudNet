package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;

public final class NukkitChannelMessageReceiveEvent extends NukkitCloudNetEvent {

    private static final HandlerList handlers = new HandlerList();

    private final String channel, message;

    private final JsonDocument data;

    public NukkitChannelMessageReceiveEvent(String channel, String message, JsonDocument data) {
        this.channel = channel;
        this.message = message;
        this.data = data;
    }

    public static HandlerList getHandlers() {
        return NukkitChannelMessageReceiveEvent.handlers;
    }

    public String getChannel() {
        return this.channel;
    }

    public String getMessage() {
        return this.message;
    }

    public JsonDocument getData() {
        return this.data;
    }
}