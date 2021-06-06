package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class BungeeBridgeProxyPlayerLoginSuccessEvent extends BungeeBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  public BungeeBridgeProxyPlayerLoginSuccessEvent(NetworkConnectionInfo networkConnectionInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }
}
