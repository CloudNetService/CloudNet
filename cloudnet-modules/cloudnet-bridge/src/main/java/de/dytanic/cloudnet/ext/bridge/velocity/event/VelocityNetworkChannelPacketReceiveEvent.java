package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class VelocityNetworkChannelPacketReceiveEvent extends
  VelocityCloudNetEvent {

  @Getter
  private final INetworkChannel channel;

  @Getter
  private final IPacket packet;
}