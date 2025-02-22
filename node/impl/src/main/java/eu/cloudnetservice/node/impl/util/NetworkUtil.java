/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.util;

import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.utils.base.StringUtil;
import java.io.IOException;
import java.net.BindException;
import java.net.IDN;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public final class NetworkUtil {

  private static final String LOCAL_ADDRESS = findLocalAddress();
  private static final Set<String> AVAILABLE_IP_ADDRESSES = resolveAvailableAddresses();

  private NetworkUtil() {
    throw new UnsupportedOperationException();
  }

  @Unmodifiable
  public static @NonNull Set<String> availableIPAddresses() {
    return AVAILABLE_IP_ADDRESSES;
  }

  public static @NonNull String localAddress() {
    return LOCAL_ADDRESS;
  }

  public static boolean isInUse(@NonNull String hostAddress, int port) {
    try (var serverSocket = new ServerSocket()) {
      // try to bind on the port, if successful the port is free
      serverSocket.setReuseAddress(true);
      serverSocket.bind(new InetSocketAddress(hostAddress, port));
      return false;
    } catch (Exception exception) {
      return true;
    }
  }

  public static boolean checkWildcard(@NonNull HostAndPort hostAndPort) {
    return InetAddresses.forString(hostAndPort.host()).isAnyLocalAddress();
  }

  public static @NonNull String removeAddressScope(@NonNull String inputAddress) {
    // check if the host address contains '%' which separates the address
    // from the associated network interface or scope id
    var scopeSeparatorIndex = inputAddress.indexOf('%');
    if (scopeSeparatorIndex != -1) {
      // network if or scope set
      return inputAddress.substring(0, scopeSeparatorIndex);
    } else {
      // raw address
      return inputAddress;
    }
  }

  public static boolean checkAssignable(@NonNull HostAndPort hostAndPort) {
    try (var socket = new ServerSocket()) {
      // try to bind on the given address
      socket.setReuseAddress(true);
      socket.bind(new InetSocketAddress(hostAndPort.host(), 0));
      return true;
    } catch (IOException exception) {
      return exception instanceof BindException
        && exception.getMessage() != null
        && exception.getMessage().startsWith("Address already in use");
    }
  }

  public static @Nullable HostAndPort parseAssignableHostAndPort(@NonNull String address, boolean withPort) {
    // try to parse host and port from the given string
    var hostAndPort = parseHostAndPort(address, withPort);
    return hostAndPort != null && checkAssignable(hostAndPort) ? hostAndPort : null;
  }

  public static @Nullable HostAndPort parseHostAndPort(@NonNull String input, boolean withPort) {
    // convert the input to an ascii string if needed (for example ☃.net -> xn--n3h.net)
    var normalizedInput = StringUtil.toLower(IDN.toASCII(input, IDN.ALLOW_UNASSIGNED));

    // extract the port from the input if required
    var port = -1;
    if (withPort) {
      var portSeparatorIndex = normalizedInput.lastIndexOf(':');
      if (portSeparatorIndex == -1) {
        // missing port
        return null;
      }

      // extract the port part
      var portPart = normalizedInput.substring(portSeparatorIndex + 1);
      if (portPart.isEmpty()) {
        // missing port
        return null;
      }

      // try to get the port
      var possiblePort = Ints.tryParse(portPart);
      if (possiblePort == null || possiblePort < 0 || possiblePort > 0xFFFF) {
        // invalid port
        return null;
      }

      // store the port and remove the port part from the input string
      port = possiblePort;
      normalizedInput = normalizedInput.substring(0, portSeparatorIndex);
    }

    // check if the host is wrapped in brackets
    if (normalizedInput.startsWith("[")) {
      normalizedInput = normalizedInput.substring(1);
    }

    // extracting this check allows accidental typos to happen like [2001:db8::1
    if (normalizedInput.endsWith("]")) {
      normalizedInput = normalizedInput.substring(0, normalizedInput.length() - 1);
    }

    try {
      // try to parse an ipv 4 or 6 address from the input string
      var address = InetAddresses.forString(normalizedInput);
      return new HostAndPort(extractHostAddress(address), port);
    } catch (IllegalArgumentException ignored) {
    }

    try {
      // not the end of the world - might still be a domain name
      var address = InetAddress.getByName(normalizedInput);
      return new HostAndPort(extractHostAddress(address), port);
    } catch (UnknownHostException exception) {
      // okay that's it
      return null;
    }
  }

  private static @NonNull String findLocalAddress() {
    try {
      return extractHostAddress(InetAddress.getLocalHost());
    } catch (UnknownHostException exception) {
      return "127.0.0.1";
    }
  }

  private static @NonNull String extractHostAddress(@NonNull InetAddress address) {
    if (address instanceof Inet6Address) {
      // get the host address of the inet address
      var hostAddress = address.getHostAddress();
      return removeAddressScope(hostAddress);
    } else {
      // just add the address
      return address.getHostAddress();
    }
  }

  private static @NonNull Set<String> resolveAvailableAddresses() {
    try {
      Set<String> addresses = new HashSet<>();
      // try to resolve all ip addresses available on the system
      var networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        // get all addresses of the interface
        var inetAddresses = networkInterfaces.nextElement().getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
          addresses.add(extractHostAddress(inetAddresses.nextElement()));
        }
      }
      // return the located addresses
      return Set.copyOf(addresses);
    } catch (SocketException exception) {
      return Set.of("127.0.0.1", "127.0.1.1");
    }
  }
}
