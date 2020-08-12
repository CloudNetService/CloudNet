package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.IPacketSender;
import org.jetbrains.annotations.NotNull;

/**
 * A NetworkChannel instance represents an open connection
 */
public interface INetworkChannel extends IPacketSender, AutoCloseable {

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
    INetworkChannelHandler getHandler();

    /**
     * Sets the channel handler for the channels. That is important for the handling of
     * receiving packets or channel closing and connect handler
     *
     * @param handler the handler, that should handle this channel
     */
    void setHandler(INetworkChannelHandler handler);

    /**
     * Returns the own packet listener registry. The packetRegistry is a sub registry of
     * the network component packet listener registry
     */
    IPacketListenerRegistry getPacketRegistry();

    /**
     * Returns that, the channel based of the client site connection
     */
    boolean isClientProvidedChannel();

    ITask<IPacket> sendQueryAsync(@NotNull IPacket packet);

    IPacket sendQuery(@NotNull IPacket packet);

    boolean isWriteable();

    boolean isActive();
}