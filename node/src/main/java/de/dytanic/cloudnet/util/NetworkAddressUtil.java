/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.util;

import com.google.common.collect.ImmutableSet;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import lombok.NonNull;

public final class NetworkAddressUtil {

  private static final String LOCAL_ADDRESS = findLocalAddress();

  private NetworkAddressUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull Set<String> availableIPAddresses() {
    try {
      Set<String> addresses = new HashSet<>();
      // try to resolve all ip addresses available on the system
      var networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        // get all addresses of the interface
        var inetAddresses = networkInterfaces.nextElement().getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
          addresses.add(hostAddress(inetAddresses.nextElement()));
        }
      }
      // return the located addresses
      return addresses;
    } catch (SocketException exception) {
      return ImmutableSet.of("127.0.0.1", "127.0.1.1");
    }
  }

  public static String localAddress() {
    return LOCAL_ADDRESS;
  }

  private static @NonNull String findLocalAddress() {
    try {
      return hostAddress(InetAddress.getLocalHost());
    } catch (UnknownHostException exception) {
      return "127.0.0.1";
    }
  }

  private static @NonNull String hostAddress(@NonNull InetAddress address) {
    if (address instanceof Inet6Address) {
      // get the host address of the inet address
      var hostAddress = address.getHostAddress();
      // check if the host address contains '%' which separates the address from the source adapter name
      var percentile = hostAddress.indexOf('%');
      if (percentile != -1) {
        // strip the host address from the "full" address
        hostAddress = hostAddress.substring(0, percentile);
      }
      // the "better" host address
      return hostAddress;
    } else {
      // just add the address
      return address.getHostAddress();
    }
  }
}
