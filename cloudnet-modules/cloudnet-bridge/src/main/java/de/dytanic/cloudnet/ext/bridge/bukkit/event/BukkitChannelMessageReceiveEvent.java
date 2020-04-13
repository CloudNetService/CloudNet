package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class BukkitChannelMessageReceiveEvent extends BukkitCloudNetEvent {

    private static final HandlerList handlerList = new HandlerList();

    private final String channel, message;

    private final JsonDocument data;

    public BukkitChannelMessageReceiveEvent(String channel, String message, JsonDocument data) {
        this.channel = channel;
        this.message = message;
        this.data = data;
    }

    public static HandlerList getHandlerList() {
        return BukkitChannelMessageReceiveEvent.handlerList;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
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