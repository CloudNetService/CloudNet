/*
 * Copyright 2019-2022 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.driver.network;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * Represents an immutable host and port mapping. Validation of a host and port is up to the caller. A host of this
 * class might be an ipv4/ipv6 address, but can also be the path to a unix domain socket.
 *
 * @since 4.0
 */
public record HostAndPort(@NonNull String host, int port) {

  public static final int NO_PORT = -1;

  /**
   * Constructs a new host and port instance, validating the port.
   *
   * @param host the host of the address.
   * @param port the port of the address, or -1 if no port is given.
   * @throws NullPointerException     if the given host is null.
   * @throws IllegalArgumentException if the given port exceeds the port range.
   */
  public HostAndPort {
    Preconditions.checkArgument(this.port() >= -1 && this.port() <= 0xFFFF, "invalid port given");
  }

  /**
   * Tries to convert the given socket address into a host and port, throwing an exception if not possible.
   *
   * @param socketAddress the socket address to convert.
   * @return the created host and port based on the given address.
   * @throws NullPointerException     if the given socket address is null.
   * @throws IllegalArgumentException if the given socket address type cannot be converted.
   */
  @Contract("_ -> new")
  public static @NonNull HostAndPort fromSocketAddress(@NonNull SocketAddress socketAddress) {
    // inet socket address
    if (socketAddress instanceof InetSocketAddress inet) {
      return new HostAndPort(inet.getAddress().getHostAddress(), inet.getPort());
    }
    // unix socket address
    if (socketAddress instanceof UnixDomainSocketAddress unix) {
      return new HostAndPort(unix.getPath().toString(), NO_PORT);
    }
    // unsupported
    throw new IllegalArgumentException("Unsupported socket address type: " + socketAddress.getClass().getName());
  }

  /**
   * Checks if this host and port has a port provided. No port might be provided if the connection comes for example
   * from a unix domain socket.
   *
   * @return true if this host and port has a port given, false otherwise.
   */
  public boolean validPort() {
    return this.port != NO_PORT;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return this.host + ":" + this.port;
  }
}
