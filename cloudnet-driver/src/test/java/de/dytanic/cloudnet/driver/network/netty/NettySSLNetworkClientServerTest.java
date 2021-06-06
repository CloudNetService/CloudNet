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

package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.INetworkServer;
import de.dytanic.cloudnet.driver.network.netty.client.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.netty.server.NettyNetworkServer;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public final class NettySSLNetworkClientServerTest implements INetworkChannelHandler {

  @Test
  public void testSslNetworking() throws Exception {
    int port = NettyTestUtil.generateRandomPort();

    SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();

    INetworkServer server = new NettyNetworkServer(new SSLConfiguration(
      true,
      null,
      selfSignedCertificate.certificate().toPath(),
      selfSignedCertificate.privateKey().toPath()
    ), () -> this);

    INetworkClient client = new NettyNetworkClient(() -> this, new SSLConfiguration(
      false,
      null,
      selfSignedCertificate.certificate().toPath(),
      selfSignedCertificate.privateKey().toPath()
    ));

    Assert.assertTrue(server.isSslEnabled());
    Assert.assertTrue(client.isSslEnabled());

    ITask<String> task = new ListenableTask<>(() -> "Hello, world!");

    server.getPacketRegistry().addListener(1, (channel, packet) -> {
      if (packet.getHeader().contains("hello") && packet.getHeader().getString("hello").equalsIgnoreCase("Unit test") &&
        new String(packet.getBodyAsArray()).equalsIgnoreCase("Test Test Test 1 2 4")) {
        task.call();
      }
    });

    HostAndPort hostAndPort = new HostAndPort("127.0.0.1", port);

    Assert.assertTrue(server.addListener(hostAndPort));
    Assert.assertTrue(client.connect(hostAndPort));

    Assert.assertEquals("Hello, world!", task.get(5, TimeUnit.SECONDS));

    server.close();
    client.close();
  }

  @Override
  public void handleChannelInitialize(INetworkChannel channel) throws Exception {
    channel.sendPacket(new Packet(1, new JsonDocument("hello", "Unit test"), "Test Test Test 1 2 4".getBytes()));
  }

  @Override
  public boolean handlePacketReceive(INetworkChannel channel, Packet packet) throws Exception {
    return true;
  }

  @Override
  public void handleChannelClose(INetworkChannel channel) throws Exception {

  }
}
