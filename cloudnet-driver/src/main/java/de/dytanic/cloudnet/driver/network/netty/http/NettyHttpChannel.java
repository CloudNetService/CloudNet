package de.dytanic.cloudnet.driver.network.netty.http;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.http.IHttpChannel;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class NettyHttpChannel implements IHttpChannel {

  protected final Channel channel;

  protected final HostAndPort serverAddress, clientAddress;

  public NettyHttpChannel(Channel channel, HostAndPort serverAddress, HostAndPort clientAddress) {
    this.channel = channel;
    this.serverAddress = serverAddress;
    this.clientAddress = clientAddress;
  }

  @Override
  public HostAndPort serverAddress() {
    return this.serverAddress;
  }

  @Override
  public HostAndPort clientAddress() {
    return this.clientAddress;
  }

  @Override
  public void close() {
    this.channel.close();
  }

  public Channel getChannel() {
    return this.channel;
  }

  public HostAndPort getServerAddress() {
    return this.serverAddress;
  }

  public HostAndPort getClientAddress() {
    return this.clientAddress;
  }
}
