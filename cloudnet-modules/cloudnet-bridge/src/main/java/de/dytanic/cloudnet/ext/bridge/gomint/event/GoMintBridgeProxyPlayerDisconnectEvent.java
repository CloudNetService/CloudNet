package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class GoMintBridgeProxyPlayerDisconnectEvent extends
  GoMintBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

}