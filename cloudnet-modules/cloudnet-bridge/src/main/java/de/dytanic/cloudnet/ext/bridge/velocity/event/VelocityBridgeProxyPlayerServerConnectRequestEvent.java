package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;

public class VelocityBridgeProxyPlayerServerConnectRequestEvent extends VelocityBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  private final NetworkServiceInfo networkServiceInfo;

  public VelocityBridgeProxyPlayerServerConnectRequestEvent(NetworkConnectionInfo networkConnectionInfo,
    NetworkServiceInfo networkServiceInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
    this.networkServiceInfo = networkServiceInfo;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }

  public NetworkServiceInfo getNetworkServiceInfo() {
    return this.networkServiceInfo;
  }
}
