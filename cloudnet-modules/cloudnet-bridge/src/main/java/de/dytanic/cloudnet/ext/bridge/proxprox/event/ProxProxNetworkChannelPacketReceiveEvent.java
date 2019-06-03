package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ProxProxNetworkChannelPacketReceiveEvent extends
    ProxProxCloudNetEvent {

  @Getter
  private final INetworkChannel channel;

  @Getter
  private final IPacket packet;
}