package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SpongeNetworkChannelPacketReceiveEvent extends
  SpongeCloudNetEvent {

  @Getter
  private final INetworkChannel channel;

  @Getter
  private final IPacket packet;
}