package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class NettyNetworkClientInitializer extends ChannelInitializer<Channel> {

  private final NettyNetworkClient nettyNetworkClient;

  private final HostAndPort hostAndPort;

  @Override
  protected void initChannel(Channel ch) throws Exception {
    if (nettyNetworkClient.sslContext != null) {
      ch.pipeline()
          .addLast(nettyNetworkClient.sslContext
              .newHandler(ch.alloc(), hostAndPort.getHost(),
                  hostAndPort.getPort()));
    }

    ch.pipeline()
        .addLast("packet-length-deserializer",
            new NettyPacketLengthDeserializer())
        .addLast("packet-decoder", new NettyPacketDecoder())
        .addLast("packet-length-serializer", new NettyPacketLengthSerializer())
        .addLast("packet-encoder", new NettyPacketEncoder())
        .addLast("network-client-handler",
            new NettyNetworkClientHandler(nettyNetworkClient, hostAndPort))
    ;
  }
}