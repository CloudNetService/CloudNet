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

package eu.cloudnetservice.node.impl.cluster.util;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.impl.service.InternalCloudServiceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public final class NodeDisconnectHandlerTest {

  private static final String GRACE_PERIOD_PROPERTY = "cloudnet.max.node.disconnect.millis";

  private static NodeDisconnectHandler createHandler(long gracePeriodMillis) {
    System.setProperty(GRACE_PERIOD_PROPERTY, Long.toString(gracePeriodMillis));
    return new NodeDisconnectHandler(
      Mockito.mock(I18n.class),
      Mockito.mock(EventManager.class),
      Mockito.mock(InternalCloudServiceManager.class));
  }

  private static NodeServer mockNodeServer(boolean available, boolean head) {
    var server = Mockito.mock(NodeServer.class);
    Mockito.when(server.name()).thenReturn("Node-2");
    Mockito.when(server.available()).thenReturn(available);
    Mockito.when(server.head()).thenReturn(head);
    Mockito.when(server.channel()).thenReturn(Mockito.mock(NetworkChannel.class));
    Mockito.when(server.provider()).thenReturn(Mockito.mock(NodeServerProvider.class));
    return server;
  }

  @AfterEach
  void clearGracePeriod() {
    System.clearProperty(GRACE_PERIOD_PROPERTY);
  }

  @Test
  void testGracePeriodIsReadFromSystemProperty() {
    Assertions.assertEquals(0, createHandler(0).hardDisconnectMillis());
    Assertions.assertEquals(30_000, createHandler(30_000).hardDisconnectMillis());
  }

  @Test
  void testChannelCloseRemovesNodeWithoutGracePeriod() {
    var handler = createHandler(0);
    var server = mockNodeServer(true, false);

    handler.handleNodeServerChannelClose(server);

    Mockito.verify(server).close();
    Mockito.verify(server, Mockito.never()).state(Mockito.any());
  }

  @Test
  void testChannelCloseMarksNodeDisconnectedWithGracePeriod() {
    var handler = createHandler(30_000);
    var server = mockNodeServer(true, false);
    var closedChannel = server.channel();

    handler.handleNodeServerChannelClose(server);

    Mockito.verify(server, Mockito.never()).close();
    Mockito.verify(server).state(NodeServerState.DISCONNECTED);

    // packets to the node must be queued instead of being written to the closed channel
    var channelCaptor = ArgumentCaptor.forClass(NetworkChannel.class);
    Mockito.verify(server).channel(channelCaptor.capture());
    var queuedChannel = Assertions.assertInstanceOf(QueuedNetworkChannel.class, channelCaptor.getValue());
    queuedChannel.sendPacket(Mockito.mock(Packet.class));
    Mockito.verify(closedChannel, Mockito.never()).sendPacket(Mockito.any(Packet.class));
  }

  @Test
  void testChannelCloseRemovesUnavailableNodeWithGracePeriod() {
    var handler = createHandler(30_000);
    var server = mockNodeServer(false, false);

    handler.handleNodeServerChannelClose(server);

    Mockito.verify(server).close();
    Mockito.verify(server, Mockito.never()).state(Mockito.any());
  }

  @Test
  void testMarkingHeadNodeDisconnectedSelectsNewHeadNode() {
    var handler = createHandler(30_000);
    var server = mockNodeServer(true, true);

    handler.markNodeServerDisconnected(server);

    Mockito.verify(server.provider()).selectHeadNode();
  }

  @Test
  void testMarkingOtherNodeDisconnectedKeepsHeadNode() {
    var handler = createHandler(30_000);
    var server = mockNodeServer(true, false);

    handler.markNodeServerDisconnected(server);

    Mockito.verify(server.provider(), Mockito.never()).selectHeadNode();
  }
}
