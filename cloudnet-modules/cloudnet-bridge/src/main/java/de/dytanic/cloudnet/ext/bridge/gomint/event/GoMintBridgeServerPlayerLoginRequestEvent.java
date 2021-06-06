package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;

/**
 * {@inheritDoc}
 */
public final class GoMintBridgeServerPlayerLoginRequestEvent extends GoMintBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  private final NetworkPlayerServerInfo networkPlayerServerInfo;

  public GoMintBridgeServerPlayerLoginRequestEvent(NetworkConnectionInfo networkConnectionInfo,
    NetworkPlayerServerInfo networkPlayerServerInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
    this.networkPlayerServerInfo = networkPlayerServerInfo;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }

  public NetworkPlayerServerInfo getNetworkPlayerServerInfo() {
    return this.networkPlayerServerInfo;
  }
}
