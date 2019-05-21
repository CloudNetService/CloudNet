package de.dytanic.cloudnet.driver.network;

/**
 * The network client represents a client based connector to one or more remote servers.
 */
public interface INetworkClient extends INetworkComponent, AutoCloseable {

    /**
     * Open a new connection to the specific host and port
     *
     * @param hostAndPort the address, that should the client connect to
     * @return true if the connection was success or false if the connection was unsuccessful or refused
     */
    boolean connect(HostAndPort hostAndPort);
}