package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.HandlerList;

@RequiredArgsConstructor
public final class BukkitNetworkClusterNodeInfoUpdateEvent extends
    BukkitCloudNetEvent {

  @Getter
  private static HandlerList handlerList = new HandlerList();

  @Getter
  private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;

  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }
}