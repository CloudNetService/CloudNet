package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class SpongeBridgeProxyPlayerLoginSuccessEvent extends SpongeBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  public SpongeBridgeProxyPlayerLoginSuccessEvent(NetworkConnectionInfo networkConnectionInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }
}
