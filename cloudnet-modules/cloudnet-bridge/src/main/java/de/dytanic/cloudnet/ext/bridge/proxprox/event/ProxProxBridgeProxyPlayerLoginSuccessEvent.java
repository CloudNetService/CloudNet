package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ProxProxBridgeProxyPlayerLoginSuccessEvent extends ProxProxBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

}