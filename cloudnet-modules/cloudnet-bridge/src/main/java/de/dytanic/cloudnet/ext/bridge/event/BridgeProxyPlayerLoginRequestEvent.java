package de.dytanic.cloudnet.ext.bridge.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class BridgeProxyPlayerLoginRequestEvent extends DriverEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  public BridgeProxyPlayerLoginRequestEvent(NetworkConnectionInfo networkConnectionInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }
}
