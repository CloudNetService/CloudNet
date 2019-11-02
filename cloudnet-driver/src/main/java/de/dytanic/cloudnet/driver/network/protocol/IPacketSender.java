package de.dytanic.cloudnet.driver.network.protocol;

/**
 * All implementations of this interface, can send a packet into the network
 * Its not specific that the sender is a client or a server.
 */
public interface IPacketSender {

    /**
     * Transport a packet instance into the network to the receiver. The receiver will
     * handle the packet if he knows the channel and has listeners for the channel
     * <p>
     * The packet are doesn't allow to be null
     *
     * @param packet the packet, which should transport into the network
     */
    void sendPacket(IPacket packet);

    /**
     * Transport packet instances into the network to the receiver. The receiver will
     * handle the packets if he knows the channel and has listeners for the channel
     * <p>
     * A packet are doesn't allow to be null
     * All elements should be checked that be not null and send after that
     *
     * @param packets the packets, which should transport into the network
     */
    void sendPacket(IPacket... packets);

}