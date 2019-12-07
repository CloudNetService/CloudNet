package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.driver.network.protocol.PacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.PacketSender;

/**
 * A NetworkChannel instance represents an open connection
 */
@Deprecated
public interface INetworkChannel extends PacketSender, AutoCloseable {

    /**
     * Returns the unique channelId.
     * The Channel Id begins with 1 and ends with Long.MAX_VALUE
     */
    long getChannelId();

    /**
     * Returns the server address from this channel
     */
    HostAndPort getServerAddress();

    /**
     * Returns the client address from this channel
     */
    HostAndPort getClientAddress();

    /**
     * Returns the networkChannelHandler from this channel
     */
    NetworkChannelHandler getHandler();

    /**
     * Sets the channel handler for the channels. That is important for the handling of
     * receiving packets or channel closing and connect handler
     *
     * @param handler the handler, that should handle this channel
     */
    @Deprecated
    void setHandler(INetworkChannelHandler handler);

    default void setHandler(NetworkChannelHandler handler) {
        setHandler((INetworkChannelHandler) handler);
    }

    /**
     * Returns the own packet listener registry. The packetRegistry is a sub registry of
     * the network component packet listener registry
     */
    PacketListenerRegistry getPacketRegistry();

    /**
     * Returns that, the channel based of the client site connection
     */
    boolean isClientProvidedChannel();
}