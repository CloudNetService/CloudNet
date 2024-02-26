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

package eu.cloudnetservice.driver.network;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HostAndPortTest {

  @Test
  void testIpv4Address() {
    var socketAddress = new InetSocketAddress("127.0.0.1", 41000);
    var hostAndPort = HostAndPort.fromSocketAddress(socketAddress);

    Assertions.assertEquals("127.0.0.1", hostAndPort.host());
    Assertions.assertEquals(41000, hostAndPort.port());
  }

  @Test
  void testIpv6Address() {
    var socketAddress = new InetSocketAddress("::1", 41000);
    var hostAndPort = HostAndPort.fromSocketAddress(socketAddress);

    Assertions.assertEquals("0:0:0:0:0:0:0:1", hostAndPort.host());
    Assertions.assertEquals(41000, hostAndPort.port());
  }
}
