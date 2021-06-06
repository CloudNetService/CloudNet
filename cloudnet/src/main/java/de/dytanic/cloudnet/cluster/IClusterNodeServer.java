package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import org.jetbrains.annotations.NotNull;

public interface IClusterNodeServer extends NodeServer, AutoCloseable {

  void sendCustomChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument data);

  void sendCustomChannelMessage(@NotNull ChannelMessage channelMessage);

  @Override
  @NotNull IClusterNodeServerProvider getProvider();

  INetworkChannel getChannel();

  void setChannel(@NotNull INetworkChannel channel);

  boolean isConnected();

  void saveSendPacket(@NotNull IPacket packet);

  void saveSendPacketSync(@NotNull IPacket packet);

  boolean isAcceptableConnection(@NotNull INetworkChannel channel, @NotNull String nodeId);

  @Override
  default boolean isAvailable() {
    return this.getChannel() != null;
  }
}
