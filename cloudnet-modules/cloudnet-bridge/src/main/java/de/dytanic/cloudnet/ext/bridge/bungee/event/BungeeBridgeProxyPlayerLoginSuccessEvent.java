package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class BungeeBridgeProxyPlayerLoginSuccessEvent extends BungeeBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

}