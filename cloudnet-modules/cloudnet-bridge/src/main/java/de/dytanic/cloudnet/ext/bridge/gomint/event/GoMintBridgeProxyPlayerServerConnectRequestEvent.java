package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GoMintBridgeProxyPlayerServerConnectRequestEvent extends
  GoMintBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  private final NetworkServiceInfo networkServiceInfo;

}