package de.dytanic.cloudnet.driver.network;

import lombok.EqualsAndHashCode;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * This class holds a easy IP/Hostname and port configuration
 * for a server or a client bind address
 */
@EqualsAndHashCode
public class HostAndPort {

    /**
     * The host address which is configured by the constructors
     * The host string can be a IPv4, IPv6 and a string
     */
    protected String host;

    /**
     * The port is the port, which should the endpoint is bind
     */
    protected int port;

    public HostAndPort(InetSocketAddress socketAddress) {
        if (socketAddress == null) return;

        this.host = socketAddress.getAddress().getHostAddress();
        this.port = socketAddress.getPort();
    }

    public HostAndPort(SocketAddress socketAddress) {
        if (socketAddress == null) return;

        String[] address = socketAddress.toString().split(":");

        this.host = address[0].replaceFirst("/", "");
        this.port = Integer.parseInt(address[1]);
    }

    public HostAndPort(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

}