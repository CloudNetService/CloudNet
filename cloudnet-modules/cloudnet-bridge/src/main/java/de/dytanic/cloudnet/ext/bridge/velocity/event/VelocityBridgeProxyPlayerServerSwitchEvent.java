package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class VelocityBridgeProxyPlayerServerSwitchEvent extends VelocityBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    private final NetworkServiceInfo networkServiceInfo;

}