package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class NukkitCloudServiceInfoUpdateEvent extends
    NukkitCloudNetEvent {

  @Getter
  private static final HandlerList handlers = new HandlerList();

  @Getter
  private final ServiceInfoSnapshot serviceInfoSnapshot;
}