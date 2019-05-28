package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class NukkitNetworkClusterNodeInfoUpdateEvent extends
    NukkitCloudNetEvent {

  @Getter
  private static final HandlerList handlers = new HandlerList();

  @Getter
  private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;
}