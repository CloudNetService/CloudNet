package de.dytanic.cloudnet.driver.network;

/**
 * The network server represents a server that can register and receive INetworkClient connections and packets
 * It is made for a simple read and write network with a client and a server.
 * You can bind this server on more than one addresses
 *
 * @see INetworkClient
 */
public interface INetworkServer extends INetworkComponent, AutoCloseable {

    /**
     * Binds the server to a specific port with the host alias address "0.0.0.0"
     *
     * @param port the port, that the server should bind
     * @return true when the binding was successful or false if an error was threw or the port is already bind
     */
    boolean addListener(int port);

    /**
     * Binds the server to a specific address that is as parameter defined
     *
     * @param hostAndPort the address that should the server bind
     * @return true when the binding was successful or false if an error was threw or the port is already bind
     */
    boolean addListener(HostAndPort hostAndPort);
}