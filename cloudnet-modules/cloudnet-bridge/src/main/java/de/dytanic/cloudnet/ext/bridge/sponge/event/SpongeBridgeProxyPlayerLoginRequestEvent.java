package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class SpongeBridgeProxyPlayerLoginRequestEvent extends SpongeBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  public SpongeBridgeProxyPlayerLoginRequestEvent(NetworkConnectionInfo networkConnectionInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }
}
