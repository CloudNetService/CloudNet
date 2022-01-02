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

package eu.cloudnetservice.cloudnet.driver.network.netty.communication;

import eu.cloudnetservice.cloudnet.driver.DriverEnvironment;
import eu.cloudnetservice.cloudnet.driver.DriverTestUtility;
import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.cloudnet.driver.network.NetworkClient;
import eu.cloudnetservice.cloudnet.driver.network.NetworkServer;
import eu.cloudnetservice.cloudnet.driver.network.NetworkTestCase;
import eu.cloudnetservice.cloudnet.driver.network.netty.client.NettyNetworkClient;
import eu.cloudnetservice.cloudnet.driver.network.netty.server.NettyNetworkServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class NettyNetworkServerClientTest extends NetworkTestCase {

  @BeforeAll
  static void setupDriver() {
    Mockito
      .when(DriverTestUtility.mockAndSetDriverInstance().environment())
      .thenReturn(DriverEnvironment.WRAPPER);
  }

  @Test
  void testNetworkServerClientCommunication() throws Exception {
    var networkPort = this.randomFreePort();

    NetworkServer server = new NettyNetworkServer(this::newDummyHandler);
    NetworkClient client = new NettyNetworkClient(this::newDummyHandler);

    Assertions.assertTrue(server.addListener(networkPort));
    Assertions.assertTrue(client.connect(HostAndPort.fromSocketAddress(
      new InetSocketAddress(InetAddress.getLoopbackAddress(), networkPort))));

    client.close();
    server.close();
  }

  private NetworkChannelHandler newDummyHandler() {
    return Mockito.mock(NetworkChannelHandler.class);
  }
}
