package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.HandlerList;

@RequiredArgsConstructor
public final class BukkitCloudServiceStartEvent extends BukkitCloudNetEvent {

    @Getter
    private static HandlerList handlerList = new HandlerList();

    @Getter
    private final ServiceInfoSnapshot serviceInfoSnapshot;

    @Override
    public HandlerList getHandlers()
    {
        return handlerList;
    }
}