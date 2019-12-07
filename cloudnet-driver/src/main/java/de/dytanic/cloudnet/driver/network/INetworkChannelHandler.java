package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.driver.network.protocol.AbstractPacket;

/**
 * A networkChannelHandler provides the operation with the INetworkChannel
 *
 * @see INetworkChannel
 */
@Deprecated
public interface INetworkChannelHandler {

    /**
     * Handles an new open connected channel
     *
     * @param channel the providing channel on that this handler is sets on this
     */
    void handleChannelInitialize(INetworkChannel channel) throws Exception;

    default void handleChannelInitialize(NetworkChannel channel) throws Exception {
        handleChannelInitialize((INetworkChannel) channel);
    }

    /**
     * Handles a incoming packet from a provided channel, that contains that channel handler
     *
     * @param channel the providing channel on that this handler is sets on this
     * @param packet  the packet, that will received from the remote component
     * @return should return true that, the packet that was received is allowed to handle from the packet listeners at the packetListenerRegistry
     */
    @Deprecated
    boolean handlePacketReceive(INetworkChannel channel, AbstractPacket packet) throws Exception;

    default boolean handlePacketReceive(NetworkChannel channel, AbstractPacket packet) throws Exception {
        return handlePacketReceive((INetworkChannel) channel, packet);
    }

    /**
     * Handles the close phase from a NetworkChannel
     *
     * @param channel the providing channel on that this handler is sets on this
     */
    @Deprecated
    void handleChannelClose(INetworkChannel channel) throws Exception;

    default void handleChannelClose(NetworkChannel channel) throws Exception {
        handleChannelClose((INetworkChannel) channel);
    }
}