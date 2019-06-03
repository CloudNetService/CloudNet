package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.event.HandlerList;

@NoArgsConstructor
public final class BukkitCloudNetTickEvent extends BukkitCloudNetEvent {

    @Getter
    private static HandlerList handlerList = new HandlerList();

    @Override
    public HandlerList getHandlers()
    {
        return handlerList;
    }
}