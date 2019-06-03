package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class NukkitCloudNetTickEvent extends NukkitCloudNetEvent {

    @Getter
    private static final HandlerList handlers = new HandlerList();

}