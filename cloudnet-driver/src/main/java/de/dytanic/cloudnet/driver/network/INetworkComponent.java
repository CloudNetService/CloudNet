package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.driver.network.protocol.PacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.PacketSender;

import java.util.Collection;

/**
 * Includes the basic functions for the client and the server
 */
@Deprecated
interface INetworkComponent extends PacketSender {

    /**
     * Returns true if the network component allows to create a ssl connection
     */
    boolean isSslEnabled();

    /**
     * Returns all running enabled connections from the network component
     */
    Collection<NetworkChannel> getChannels();

    /**
     * Close all open connections from this network component
     */
    void closeChannels();

    /**
     * Returns the parent packet registry from all channels, that are this network component provide
     */
    PacketListenerRegistry getPacketRegistry();
}