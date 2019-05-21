package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NukkitBridgeProxyPlayerServerConnectRequestEvent extends NukkitBridgeEvent {

    @Getter
    private static final HandlerList handlers = new HandlerList();

    private final NetworkConnectionInfo networkConnectionInfo;

    private final NetworkServiceInfo networkServiceInfo;

}