/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.network.listener;

import eu.cloudnetservice.driver.cluster.NetworkCluster;
import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.cluster.defaults.RemoteNodeServer;
import eu.cloudnetservice.node.impl.cluster.util.NodeDisconnectHandler;
import eu.cloudnetservice.node.impl.cluster.util.QueuedNetworkChannel;
import eu.cloudnetservice.node.impl.junit.EnableServicesInject;
import eu.cloudnetservice.node.impl.network.NodeNetworkUtil;
import eu.cloudnetservice.node.service.CloudServiceManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@EnableServicesInject
public final class AuthorizationResponsePacketListenerTest {

  private static final HostAndPort NODE_LISTENER = new HostAndPort("127.0.0.1", 1410);
  private static final NetworkClusterNode NODE = new NetworkClusterNode("Node-1", List.of(NODE_LISTENER));

  private NodeServerProvider provider;
  private CloudServiceManager cloudServiceManager;
  private RemoteNodeServer server;
  private NetworkChannel oldChannel;
  private NetworkChannel newChannel;
  private AuthorizationResponsePacketListener listener;

  private static Packet plainAuthSuccessPacket() {
    var content = Mockito.mock(DataBuf.class);
    // the authorization was successful, but the other node did not handle it as a reconnect
    Mockito.when(content.readBoolean()).thenReturn(true, false);
    var packet = Mockito.mock(Packet.class);
    Mockito.when(packet.content()).thenReturn(content);
    return packet;
  }

  @BeforeEach
  void setUp() {
    this.provider = Mockito.mock(NodeServerProvider.class);
    this.server = RemoteNodeServerTestUtil.createRemoteNodeServer(
      NODE,
      this.provider,
      Mockito.mock(NodeDisconnectHandler.class));
    Mockito.when(this.provider.node(NODE.uniqueId())).thenReturn(this.server);

    this.oldChannel = RemoteNodeServerTestUtil.mockChannel();
    this.newChannel = RemoteNodeServerTestUtil.mockChannel();
    Mockito.when(this.newChannel.serverAddress()).thenReturn(NODE_LISTENER);

    var configuration = Mockito.mock(Configuration.class);
    Mockito.when(configuration.clusterConfig()).thenReturn(new NetworkCluster(UUID.randomUUID(), List.of(NODE)));
    this.cloudServiceManager = Mockito.mock(CloudServiceManager.class);
    Mockito.when(this.cloudServiceManager.localCloudServices()).thenReturn(List.of());

    this.listener = new AuthorizationResponsePacketListener(
      Mockito.mock(I18n.class),
      configuration,
      Mockito.mock(NodeNetworkUtil.class),
      Mockito.mock(DataSyncRegistry.class),
      this.provider,
      this.cloudServiceManager);
  }

  @Test
  void testNewConnectionSelectsHeadNode() {
    this.listener.handle(this.newChannel, plainAuthSuccessPacket());

    Assertions.assertEquals(NodeServerState.READY, this.server.state());
    Assertions.assertSame(this.newChannel, this.server.channel());
    Mockito.verify(this.provider).selectHeadNode();
    // this node never lost a connection to the node, there is nothing to re-publish
    Mockito.verify(this.cloudServiceManager, Mockito.never()).localCloudServices();
  }

  @Test
  void testPlainAuthAfterReconnectFlushesQueuedPackets() {
    var queuedPacket = Mockito.mock(Packet.class);
    var queuedChannel = new QueuedNetworkChannel(this.oldChannel);
    queuedChannel.sendPacket(queuedPacket);
    RemoteNodeServerTestUtil.initSession(this.server, queuedChannel, 1_000, NodeServerState.DISCONNECTED);
    Mockito.clearInvocations(this.provider);

    this.listener.handle(this.newChannel, plainAuthSuccessPacket());

    Mockito.verify(this.newChannel).sendPacketSync(queuedPacket);
    Mockito.verify(this.oldChannel).close();
    Mockito.verify(this.provider).selectHeadNode();
    // the other node might have removed the services of this node, they must be re-published
    Mockito.verify(this.cloudServiceManager).localCloudServices();
    Assertions.assertEquals(NodeServerState.READY, this.server.state());
    Assertions.assertSame(this.newChannel, this.server.channel());
  }
}
