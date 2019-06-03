package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.http.IHttpChannel;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
final class NettyHttpChannel implements IHttpChannel {

    protected final Channel channel;

    protected final HostAndPort serverAddress, clientAddress;

    @Override
    public HostAndPort serverAddress()
    {
        return serverAddress;
    }

    @Override
    public HostAndPort clientAddress()
    {
        return clientAddress;
    }

    @Override
    public void close() throws Exception
    {
        channel.close();
    }
}