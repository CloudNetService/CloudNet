package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public final class BukkitBridgeProxyPlayerServerSwitchEvent extends
    BukkitBridgeEvent {

  private final NetworkConnectionInfo networkConnectionInfo;

  private final NetworkServiceInfo networkServiceInfo;

  @Getter
  private static HandlerList handlerList = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }

}