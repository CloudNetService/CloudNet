package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class NukkitBridgeConfigurationUpdateEvent extends
    NukkitBridgeEvent {

  @Getter
  private static final HandlerList handlers = new HandlerList();

  private final BridgeConfiguration bridgeConfiguration;

}