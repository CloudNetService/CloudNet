package de.dytanic.cloudnet.driver.network;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * This class holds a easy IP/Hostname and port configuration
 * for a server or a client bind address
 */
@Getter
@EqualsAndHashCode
@AllArgsConstructor
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

    @Override
    public String toString() {
        return host + ":" + port;
    }
}