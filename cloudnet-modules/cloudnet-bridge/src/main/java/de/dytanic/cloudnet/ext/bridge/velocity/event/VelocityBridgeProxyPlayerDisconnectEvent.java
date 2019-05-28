package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class VelocityBridgeProxyPlayerDisconnectEvent extends
  VelocityBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

}