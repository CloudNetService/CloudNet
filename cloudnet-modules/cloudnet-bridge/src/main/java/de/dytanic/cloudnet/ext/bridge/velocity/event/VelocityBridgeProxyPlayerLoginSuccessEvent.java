package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class VelocityBridgeProxyPlayerLoginSuccessEvent extends VelocityBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  public VelocityBridgeProxyPlayerLoginSuccessEvent(NetworkConnectionInfo networkConnectionInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }
}
