package de.dytanic.cloudnet.driver.network.netty.client;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketDecoder;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketEncoder;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketLengthDeserializer;
import de.dytanic.cloudnet.driver.network.netty.codec.NettyPacketLengthSerializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
final class NettyNetworkClientInitializer extends ChannelInitializer<Channel> {

  private final NettyNetworkClient nettyNetworkClient;
  private final HostAndPort hostAndPort;

  private final Runnable handler;

  public NettyNetworkClientInitializer(NettyNetworkClient nettyNetworkClient, HostAndPort hostAndPort) {
    this(nettyNetworkClient, hostAndPort, null);
  }

  public NettyNetworkClientInitializer(NettyNetworkClient nettyNetworkClient, HostAndPort hostAndPort,
    Runnable handler) {
    this.nettyNetworkClient = nettyNetworkClient;
    this.hostAndPort = hostAndPort;
    this.handler = handler;
  }

  @Override
  protected void initChannel(@NotNull Channel ch) {
    if (this.nettyNetworkClient.sslContext != null) {
      ch.pipeline()
        .addLast(this.nettyNetworkClient.sslContext
          .newHandler(ch.alloc(), this.hostAndPort.getHost(), this.hostAndPort.getPort()));
    }

    ch.pipeline()
      .addLast("packet-length-deserializer", new NettyPacketLengthDeserializer())
      .addLast("packet-decoder", new NettyPacketDecoder())
      .addLast("packet-length-serializer", new NettyPacketLengthSerializer())
      .addLast("packet-encoder", new NettyPacketEncoder())
      .addLast("network-client-handler", new NettyNetworkClientHandler(this.nettyNetworkClient, this.hostAndPort));

    if (this.handler != null) {
      this.handler.run();
    }
  }
}
