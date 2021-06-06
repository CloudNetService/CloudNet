package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

/**
 * {@inheritDoc}
 */
public final class NukkitBridgeProxyPlayerDisconnectEvent extends NukkitBridgeEvent {

  private static final HandlerList handlers = new HandlerList();

  private final NetworkConnectionInfo networkConnectionInfo;

  public NukkitBridgeProxyPlayerDisconnectEvent(NetworkConnectionInfo networkConnectionInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
  }

  public static HandlerList getHandlers() {
    return NukkitBridgeProxyPlayerDisconnectEvent.handlers;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }
}
