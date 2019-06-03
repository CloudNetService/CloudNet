package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class NukkitNetworkChannelPacketReceiveEvent extends NukkitCloudNetEvent {

    @Getter
    private static final HandlerList handlers = new HandlerList();

    @Getter
    private final INetworkChannel channel;

    @Getter
    private final IPacket packet;
}