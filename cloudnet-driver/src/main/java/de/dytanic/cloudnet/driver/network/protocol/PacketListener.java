package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.driver.network.INetworkChannel;

/**
 * An packet listeners, allows to handle incoming packets, from some channel, that use the IPacketListenerRegistry in that
 * the listener has to be register
 * <p>
 * It will called on all channels, that the registry has register the listener
 *
 * @see PacketListenerRegistry
 */
public interface PacketListener {

    /**
     * Handles a new incoming packet message. The channel and the packet will not null
     *
     * @param channel the channel, from that the message was received
     * @param packet  the received packet message, which should handle from the listener
     * @throws Exception catch the exception, if the handle throws one
     */
    void handle(INetworkChannel channel, Packet packet) throws Exception;
}