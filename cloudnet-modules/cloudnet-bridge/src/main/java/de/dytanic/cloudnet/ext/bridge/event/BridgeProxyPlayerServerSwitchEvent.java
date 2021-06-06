package de.dytanic.cloudnet.ext.bridge.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;

public final class BridgeProxyPlayerServerSwitchEvent extends DriverEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  private final NetworkServiceInfo networkServiceInfo;

  public BridgeProxyPlayerServerSwitchEvent(NetworkConnectionInfo networkConnectionInfo,
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
