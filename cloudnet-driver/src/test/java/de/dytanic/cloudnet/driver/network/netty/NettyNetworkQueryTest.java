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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.INetworkServer;
import de.dytanic.cloudnet.driver.network.def.internal.InternalSyncPacketChannel;
import de.dytanic.cloudnet.driver.network.netty.client.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.netty.server.NettyNetworkServer;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import org.junit.Assert;
import org.junit.Test;

public class NettyNetworkQueryTest {

  private boolean connectedClient = false;
  private boolean connectedServer = false;

  @Test
  public void testNettyConnectorServer() throws Throwable {
    INetworkServer networkServer = new NettyNetworkServer(NetworkChannelServerHandler::new);
    INetworkClient networkClient = new NettyNetworkClient(NetworkChannelClientHandler::new);

    networkClient.getPacketRegistry().addListener(6, new PacketListenerImpl());
    networkServer.getPacketRegistry().addListener(6, new PacketListenerImpl());

    HostAndPort address = new HostAndPort("127.0.0.1", NettyTestUtil.generateRandomPort());

    Assert.assertTrue(networkServer.addListener(address));
    Assert.assertTrue(networkClient.connect(address));

    Thread.sleep(50);
    Assert.assertTrue(this.connectedClient);
    Assert.assertTrue(this.connectedServer);

    Assert.assertEquals(1, networkClient.getChannels().size());
    Assert.assertEquals(1, networkServer.getChannels().size());

    IPacket result = networkClient.getFirstChannel()
      .sendQuery(new Packet(6, JsonDocument.newDocument("TestKey", "TestValue")));

    Assert.assertEquals("val", result.getHeader().getString("test"));

    Assert.assertEquals(50, result.getBuffer().readInt());

    networkClient.close();
    networkServer.close();

    Assert.assertEquals(0, networkClient.getChannels().size());
    Assert.assertEquals(0, networkServer.getChannels().size());
  }

  private static final class PacketListenerImpl implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
      Assert.assertEquals(packet.getHeader().getString("TestKey"), "TestValue");
      channel.sendPacket(new Packet(-1, packet.getUniqueId(), JsonDocument.newDocument("test", "val"),
        ProtocolBuffer.create().writeInt(50)));
    }
  }

  private final class NetworkChannelServerHandler implements INetworkChannelHandler {

    @Override
    public void handleChannelInitialize(INetworkChannel channel) {
      NettyNetworkQueryTest.this.connectedServer = true;
    }

    @Override
    public boolean handlePacketReceive(INetworkChannel channel, Packet packet) {
      return true;
    }

    @Override
    public void handleChannelClose(INetworkChannel channel) {

    }
  }

  private final class NetworkChannelClientHandler implements INetworkChannelHandler {

    @Override
    public void handleChannelInitialize(INetworkChannel channel) {
      NettyNetworkQueryTest.this.connectedClient = true;
    }

    @Override
    public boolean handlePacketReceive(INetworkChannel channel, Packet packet) {
      return !InternalSyncPacketChannel.handleIncomingChannel(channel, packet);
    }

    @Override
    public void handleChannelClose(INetworkChannel channel) {

    }
  }

}
