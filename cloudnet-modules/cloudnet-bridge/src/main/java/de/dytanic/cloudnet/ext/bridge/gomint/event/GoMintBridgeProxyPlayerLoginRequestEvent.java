package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

/**
 * {@inheritDoc}
 */
public final class GoMintBridgeProxyPlayerLoginRequestEvent extends GoMintBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  public GoMintBridgeProxyPlayerLoginRequestEvent(NetworkConnectionInfo networkConnectionInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }
}
