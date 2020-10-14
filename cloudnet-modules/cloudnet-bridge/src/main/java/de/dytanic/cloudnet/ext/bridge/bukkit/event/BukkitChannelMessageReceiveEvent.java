package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.WrappedChannelMessageReceiveEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class BukkitChannelMessageReceiveEvent extends BukkitCloudNetEvent implements WrappedChannelMessageReceiveEvent {

    private static final HandlerList handlerList = new HandlerList();

    private final ChannelMessageReceiveEvent event;

    public BukkitChannelMessageReceiveEvent(ChannelMessageReceiveEvent event) {
        this.event = event;
    }

    @Override
    public ChannelMessageReceiveEvent getWrapped() {
        return this.event;
    }

    public static HandlerList getHandlerList() {
        return BukkitChannelMessageReceiveEvent.handlerList;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}