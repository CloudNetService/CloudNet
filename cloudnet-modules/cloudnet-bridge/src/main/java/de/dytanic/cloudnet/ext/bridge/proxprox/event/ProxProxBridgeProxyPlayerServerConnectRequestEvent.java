package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProxProxBridgeProxyPlayerServerConnectRequestEvent extends ProxProxBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    private final NetworkServiceInfo networkServiceInfo;

}