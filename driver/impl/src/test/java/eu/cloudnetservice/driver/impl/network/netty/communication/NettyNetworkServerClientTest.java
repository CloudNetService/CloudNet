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

package eu.cloudnetservice.driver.impl.network.netty.communication;

import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.impl.network.NetworkTestCase;
import eu.cloudnetservice.driver.impl.network.netty.client.NettyNetworkClient;
import eu.cloudnetservice.driver.impl.network.netty.server.NettyNetworkServer;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.NetworkServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class NettyNetworkServerClientTest extends NetworkTestCase {

  @Test
  void testNetworkServerClientCommunication() throws Exception {
    var networkPort = randomFreePort();
    var componentInfo = new ComponentInfo(DriverEnvironment.WRAPPER, "Testing", "Testing-Node");

    NetworkServer server = new NettyNetworkServer(componentInfo, this::newDummyHandler);
    NetworkClient client = new NettyNetworkClient(componentInfo, this::newDummyHandler);

    Assertions.assertDoesNotThrow(() -> server.addListener(networkPort).join());
    Assertions.assertDoesNotThrow(() -> client.connect(
      HostAndPort.fromSocketAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), networkPort))
    ).join());

    client.close();
    server.close();
  }

  private NetworkChannelHandler newDummyHandler() {
    return Mockito.mock(NetworkChannelHandler.class);
  }
}
