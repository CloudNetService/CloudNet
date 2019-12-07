package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListenerRegistry;

@Deprecated
public class INetworkChannelAdapter implements NetworkChannel {

    private INetworkChannel original;

    public INetworkChannelAdapter(INetworkChannel original) {
        this.original = original;
    }

    @Override
    public long getChannelId() {
        return original.getChannelId();
    }

    @Override
    public HostAndPort getServerAddress() {
        return original.getServerAddress();
    }

    @Override
    public HostAndPort getClientAddress() {
        return original.getClientAddress();
    }

    @Override
    public NetworkChannelHandler getHandler() {
        return original.getHandler();
    }

    @Override
    @Deprecated
    public void setHandler(INetworkChannelHandler handler) {
        original.setHandler(handler);
    }

    @Override
    public void setHandler(NetworkChannelHandler handler) {
        original.setHandler(handler);
    }

    @Override
    public PacketListenerRegistry getPacketRegistry() {
        return original.getPacketRegistry();
    }

    @Override
    public boolean isClientProvidedChannel() {
        return original.isClientProvidedChannel();
    }

    @Override
    public void sendPacket(Packet packet) {
        original.sendPacket(packet);
    }

    @Override
    public void sendPacket(Packet... packets) {
        original.sendPacket(packets);
    }

    @Override
    public void close() throws Exception {
        original.close();
    }
}
