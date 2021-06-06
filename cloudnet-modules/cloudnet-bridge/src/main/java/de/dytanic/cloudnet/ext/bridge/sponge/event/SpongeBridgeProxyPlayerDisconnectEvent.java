package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class SpongeBridgeProxyPlayerDisconnectEvent extends SpongeBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  public SpongeBridgeProxyPlayerDisconnectEvent(NetworkConnectionInfo networkConnectionInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }
}
