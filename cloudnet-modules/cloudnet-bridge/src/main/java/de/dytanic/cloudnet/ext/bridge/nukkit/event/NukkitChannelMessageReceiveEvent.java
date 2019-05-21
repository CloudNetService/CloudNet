package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class NukkitChannelMessageReceiveEvent extends NukkitCloudNetEvent {

    @Getter
    private static final HandlerList handlers = new HandlerList();

    @Getter
    private final String channel, message;

    @Getter
    private final JsonDocument data;
}