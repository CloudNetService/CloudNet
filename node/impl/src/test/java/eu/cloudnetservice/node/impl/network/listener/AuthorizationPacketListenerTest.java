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
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.network.standard.AuthorizationPacket;
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
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@EnableServicesInject
public final class AuthorizationPacketListenerTest {

  private static final UUID CLUSTER_ID = UUID.randomUUID();
  private static final NetworkClusterNode NODE = new NetworkClusterNode(
    "Node-1",
    List.of(new HostAndPort("127.0.0.1", 1410)));
  private static final long NODE_STARTUP_MILLIS = 1_000;

  private NodeServerProvider provider;
  private NodeDisconnectHandler disconnectHandler;
  private RemoteNodeServer server;
  private NetworkChannel oldChannel;
  private NetworkChannel newChannel;
  private AuthorizationPacketListener listener;

  private static Packet nodeAuthPacket(long startupMillis) {
    var content = Mockito.mock(DataBuf.class);
    Mockito.when(content.readUniqueId()).thenReturn(CLUSTER_ID);
    Mockito.when(content.readObject(NetworkClusterNode.class)).thenReturn(NODE);
    // nodes running an older version do not send their startup time
    Mockito.when(content.readableBytes()).thenReturn(startupMillis == -1 ? 0 : Long.BYTES);
    Mockito.when(content.readLong()).thenReturn(startupMillis);

    var packetContent = Mockito.mock(DataBuf.class);
    Mockito.when(packetContent.readObject(AuthorizationPacket.PacketAuthorizationType.class))
      .thenReturn(AuthorizationPacket.PacketAuthorizationType.NODE_TO_NODE);
    Mockito.when(packetContent.readDataBuf()).thenReturn(content);

    var packet = Mockito.mock(Packet.class);
    Mockito.when(packet.content()).thenReturn(packetContent);
    return packet;
  }

  @BeforeEach
  void setUp() {
    this.provider = Mockito.mock(NodeServerProvider.class);
    this.disconnectHandler = Mockito.mock(NodeDisconnectHandler.class);
    this.server = RemoteNodeServerTestUtil.createRemoteNodeServer(NODE, this.provider, this.disconnectHandler);
    Mockito.when(this.provider.nodeServers()).thenReturn(List.of(this.server));

    this.oldChannel = RemoteNodeServerTestUtil.mockChannel();
    this.newChannel = RemoteNodeServerTestUtil.mockChannel();

    var configuration = Mockito.mock(Configuration.class);
    Mockito.when(configuration.clusterConfig()).thenReturn(new NetworkCluster(CLUSTER_ID, List.of(NODE)));
    var dataSyncRegistry = Mockito.mock(DataSyncRegistry.class);
    Mockito.when(dataSyncRegistry.prepareClusterData(Mockito.eq(true), Mockito.any(Predicate.class)))
      .thenAnswer(_ -> DataBuf.empty());

    this.listener = new AuthorizationPacketListener(
      Mockito.mock(I18n.class),
      Mockito.mock(EventManager.class),
      configuration,
      Mockito.mock(NodeNetworkUtil.class),
      dataSyncRegistry,
      this.provider,
      Mockito.mock(CloudServiceManager.class));
  }

  @Test
  void testDisconnectedNodeStartsReconnect() {
    var queuedChannel = new QueuedNetworkChannel(this.oldChannel);
    RemoteNodeServerTestUtil.initSession(
      this.server, queuedChannel, NODE_STARTUP_MILLIS, NodeServerState.DISCONNECTED);

    this.listener.handle(this.newChannel, nodeAuthPacket(NODE_STARTUP_MILLIS));

    // the old session is kept, the channel gets switched when the sync of the reconnect completes
    Assertions.assertEquals(NodeServerState.SYNCING, this.server.state());
    Assertions.assertSame(queuedChannel, this.server.channel());
    Mockito.verify(this.disconnectHandler, Mockito.never()).handleNodeServerClose(Mockito.any());
  }

  @Test
  void testDisconnectedNodeOfOlderVersionStartsReconnect() {
    RemoteNodeServerTestUtil.initSession(
      this.server, new QueuedNetworkChannel(this.oldChannel), NODE_STARTUP_MILLIS, NodeServerState.DISCONNECTED);

    this.listener.handle(this.newChannel, nodeAuthPacket(-1));

    Assertions.assertEquals(NodeServerState.SYNCING, this.server.state());
    Mockito.verify(this.disconnectHandler, Mockito.never()).handleNodeServerClose(Mockito.any());
  }

  @Test
  void testRestartedDisconnectedNodeJoinsAsNewNode() {
    RemoteNodeServerTestUtil.initSession(
      this.server, new QueuedNetworkChannel(this.oldChannel), NODE_STARTUP_MILLIS, NodeServerState.DISCONNECTED);

    this.listener.handle(this.newChannel, nodeAuthPacket(NODE_STARTUP_MILLIS + 5_000));

    // the previous session gets closed and the node is accepted like a newly started node
    Mockito.verify(this.disconnectHandler).handleNodeServerClose(this.server);
    Mockito.verify(this.oldChannel).close();
    Assertions.assertEquals(NodeServerState.READY, this.server.state());
    Assertions.assertSame(this.newChannel, this.server.channel());
    Assertions.assertNull(this.server.nodeInfoSnapshot());
  }

  @Test
  void testRestartedReadyNodeJoinsAsNewNode() {
    RemoteNodeServerTestUtil.initSession(this.server, this.oldChannel, NODE_STARTUP_MILLIS, NodeServerState.READY);

    this.listener.handle(this.newChannel, nodeAuthPacket(NODE_STARTUP_MILLIS + 5_000));

    Mockito.verify(this.disconnectHandler).handleNodeServerClose(this.server);
    Assertions.assertEquals(NodeServerState.READY, this.server.state());
    Assertions.assertSame(this.newChannel, this.server.channel());
  }

  @Test
  void testInterruptedReconnectFlushesQueuedPackets() {
    var queuedPacket = Mockito.mock(Packet.class);
    var queuedChannel = new QueuedNetworkChannel(this.oldChannel);
    queuedChannel.sendPacket(queuedPacket);
    RemoteNodeServerTestUtil.initSession(this.server, queuedChannel, NODE_STARTUP_MILLIS, NodeServerState.SYNCING);
    Mockito.clearInvocations(this.provider);

    this.listener.handle(this.newChannel, nodeAuthPacket(NODE_STARTUP_MILLIS));

    Mockito.verify(this.newChannel).sendPacketSync(queuedPacket);
    Mockito.verify(this.oldChannel).close();
    Mockito.verify(this.provider).selectHeadNode();
    Mockito.verify(this.disconnectHandler, Mockito.never()).handleNodeServerClose(Mockito.any());
    Assertions.assertEquals(NodeServerState.READY, this.server.state());
    Assertions.assertSame(this.newChannel, this.server.channel());
  }
}
