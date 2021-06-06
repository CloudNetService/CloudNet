package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * {@inheritDoc}
 */
public final class BukkitBridgeServerPlayerLoginSuccessEvent extends BukkitBridgeEvent {

  private static final HandlerList handlerList = new HandlerList();
  private final NetworkConnectionInfo networkConnectionInfo;
  private final NetworkPlayerServerInfo networkPlayerServerInfo;

  public BukkitBridgeServerPlayerLoginSuccessEvent(NetworkConnectionInfo networkConnectionInfo,
    NetworkPlayerServerInfo networkPlayerServerInfo) {
    this.networkConnectionInfo = networkConnectionInfo;
    this.networkPlayerServerInfo = networkPlayerServerInfo;
  }

  public static HandlerList getHandlerList() {
    return BukkitBridgeServerPlayerLoginSuccessEvent.handlerList;
  }

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }

  public NetworkConnectionInfo getNetworkConnectionInfo() {
    return this.networkConnectionInfo;
  }

  public NetworkPlayerServerInfo getNetworkPlayerServerInfo() {
    return this.networkPlayerServerInfo;
  }
}
