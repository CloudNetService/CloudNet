package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.HandlerList;

@RequiredArgsConstructor
public final class BukkitNetworkChannelPacketReceiveEvent extends
  BukkitCloudNetEvent {

  @Getter
  private static HandlerList handlerList = new HandlerList();

  @Getter
  private final INetworkChannel channel;

  @Getter
  private final IPacket packet;

  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }
}