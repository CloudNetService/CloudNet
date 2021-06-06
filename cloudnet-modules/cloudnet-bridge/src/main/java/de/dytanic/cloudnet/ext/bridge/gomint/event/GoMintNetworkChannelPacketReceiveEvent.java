package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;

/**
 * {@inheritDoc}
 */
public final class GoMintNetworkChannelPacketReceiveEvent extends GoMintCloudNetEvent {

  private final INetworkChannel channel;

  private final IPacket packet;

  public GoMintNetworkChannelPacketReceiveEvent(INetworkChannel channel, IPacket packet) {
    this.channel = channel;
    this.packet = packet;
  }

  public INetworkChannel getChannel() {
    return this.channel;
  }

  public IPacket getPacket() {
    return this.packet;
  }
}
