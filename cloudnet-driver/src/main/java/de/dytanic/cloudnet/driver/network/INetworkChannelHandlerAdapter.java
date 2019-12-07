package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.driver.network.protocol.AbstractPacket;

@Deprecated
public class INetworkChannelHandlerAdapter implements NetworkChannelHandler {

    private INetworkChannelHandler original;

    public INetworkChannelHandlerAdapter(INetworkChannelHandler original) {
        this.original = original;
    }

    @Override
    public void handleChannelInitialize(INetworkChannel channel) throws Exception {
        original.handleChannelInitialize(channel);
    }

    @Override
    public void handleChannelInitialize(NetworkChannel channel) throws Exception {
        original.handleChannelInitialize(channel);
    }

    @Override
    @Deprecated
    public boolean handlePacketReceive(INetworkChannel channel, AbstractPacket packet) throws Exception {
        return original.handlePacketReceive(channel, packet);
    }

    @Override
    public boolean handlePacketReceive(NetworkChannel channel, AbstractPacket packet) throws Exception {
        return original.handlePacketReceive(channel, packet);
    }

    @Override
    @Deprecated
    public void handleChannelClose(INetworkChannel channel) throws Exception {
        original.handleChannelClose(channel);
    }

    @Override
    public void handleChannelClose(NetworkChannel channel) throws Exception {
        original.handleChannelClose(channel);
    }
}
