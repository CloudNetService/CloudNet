package de.dytanic.cloudnet.driver.network;

/**
 * The network server represents a server that can register and receive INetworkClient connections and packets
 * It is made for a simple read and write network with a client and a server.
 * You can bind this server on more than one addresses
 *
 * Replacement for deprecated {@link INetworkServer}
 *
 * @see INetworkClient
 */
public interface NetworkServer extends INetworkServer {

}
