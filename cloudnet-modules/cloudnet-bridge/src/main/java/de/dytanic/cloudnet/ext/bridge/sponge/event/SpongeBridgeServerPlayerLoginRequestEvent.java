package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;

public final class SpongeBridgeServerPlayerLoginRequestEvent extends SpongeBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  private final NetworkPlayerServerInfo networkPlayerServerInfo;

  public SpongeBridgeServerPlayerLoginRequestEvent(NetworkConnectionInfo networkConnectionInfo,
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
