package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * This class holds a easy IP/Hostname and port configuration for a server or a client bind address
 */
@EqualsAndHashCode
public class HostAndPort implements SerializableObject {

  /**
   * The host address which is configured by the constructors The host string can be an IPv4, IPv6 and a string
   */
  protected String host;
  /**
   * The port is the port where the object is bound on
   */
  protected int port;

  /**
   * @deprecated unsafe, doesn't work with IPv6-addresses because they contain ":", use {@link
   * HostAndPort#fromSocketAddress(SocketAddress)} instead
   */
  @Deprecated
  public HostAndPort(SocketAddress socketAddress) {
    if (socketAddress == null) {
      return;
    }

    String[] address = socketAddress.toString().split(":");

    this.host = address[0].replaceFirst("/", "");
    this.port = Integer.parseInt(address[1]);
  }

  public HostAndPort(InetSocketAddress socketAddress) {
    if (socketAddress == null) {
      return;
    }

    this.host = socketAddress.getAddress().getHostAddress();
    this.port = socketAddress.getPort();
  }

  public HostAndPort(String host, int port) {
    this.host = host.trim();
    this.port = port;
  }

  public HostAndPort() {
  }

  /**
   * Tries to cast the provided socketAddress to an InetSocketAddress and returns a new HostAndPort based on it
   *
   * @param socketAddress the socketAddress to get a new HostAndPort instance from
   * @return a new HostAndPort instance
   * @throws IllegalArgumentException if the provided socketAddress isn't instanceof InetSocketAddress
   */
  public static HostAndPort fromSocketAddress(SocketAddress socketAddress) {
    if (socketAddress instanceof InetSocketAddress) {
      return new HostAndPort((InetSocketAddress) socketAddress);
    }

    throw new IllegalArgumentException("socketAddress must be instance of InetSocketAddress!");
  }

  @Override
  public String toString() {
    return this.host + ":" + this.port;
  }

  public String getHost() {
    return this.host.trim();
  }

  public int getPort() {
    return this.port;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeOptionalString(this.host);
    buffer.writeInt(this.port);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.host = buffer.readOptionalString();
    this.port = buffer.readInt();
  }

}
