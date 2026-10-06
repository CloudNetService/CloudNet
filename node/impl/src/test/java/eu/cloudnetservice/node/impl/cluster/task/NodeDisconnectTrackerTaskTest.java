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

package eu.cloudnetservice.node.impl.cluster.task;

import eu.cloudnetservice.driver.cluster.NodeInfoSnapshot;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.node.cluster.LocalNodeServer;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.impl.cluster.util.NodeDisconnectHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public final class NodeDisconnectTrackerTaskTest {

  private static final long GRACE_PERIOD_MILLIS = 30_000;

  private NodeServerProvider provider;
  private LocalNodeServer localNode;
  private NodeDisconnectTrackerTask trackerTask;

  private static NodeInfoSnapshot mockSnapshot(long startupMillis) {
    var snapshot = Mockito.mock(NodeInfoSnapshot.class);
    Mockito.when(snapshot.startupMillis()).thenReturn(startupMillis);
    return snapshot;
  }

  @BeforeEach
  void setUp() {
    this.provider = Mockito.mock(NodeServerProvider.class);
    this.localNode = Mockito.mock(LocalNodeServer.class);
    Mockito.when(this.provider.localNode()).thenReturn(this.localNode);
    // the local node started after the remote node, so it is responsible for reconnecting
    var localSnapshot = mockSnapshot(2_000);
    Mockito.when(this.localNode.nodeInfoSnapshot()).thenReturn(localSnapshot);

    var disconnectHandler = Mockito.mock(NodeDisconnectHandler.class);
    Mockito.when(disconnectHandler.hardDisconnectMillis()).thenReturn(GRACE_PERIOD_MILLIS);
    this.trackerTask = new NodeDisconnectTrackerTask(Mockito.mock(I18n.class), this.provider, disconnectHandler);
  }

  private NodeServer registerRemoteNode(NodeServerState state, Instant lastStateChange) {
    var server = Mockito.mock(NodeServer.class);
    var snapshot = mockSnapshot(1_000);
    Mockito.when(server.name()).thenReturn("Node-1");
    Mockito.when(server.state()).thenReturn(state);
    Mockito.when(server.lastStateChange()).thenReturn(lastStateChange);
    Mockito.when(server.nodeInfoSnapshot()).thenReturn(snapshot);
    Mockito.when(server.connect()).thenReturn(CompletableFuture.completedFuture(null));
    Mockito.when(this.provider.nodeServers()).thenReturn(List.of(this.localNode, server));
    return server;
  }

  @Test
  void testDisconnectedNodeIsReconnectedByYoungerNode() {
    var server = this.registerRemoteNode(NodeServerState.DISCONNECTED, Instant.now());

    this.trackerTask.run();

    Mockito.verify(server).connect();
    Mockito.verify(server, Mockito.never()).close();
  }

  @Test
  void testDisconnectedNodeIsNotReconnectedByOlderNode() {
    var server = this.registerRemoteNode(NodeServerState.DISCONNECTED, Instant.now());
    var localSnapshot = mockSnapshot(500);
    Mockito.when(this.localNode.nodeInfoSnapshot()).thenReturn(localSnapshot);

    this.trackerTask.run();

    Mockito.verify(server, Mockito.never()).connect();
    Mockito.verify(server, Mockito.never()).close();
  }

  @Test
  void testReconnectIsNotRepeatedWhileHandshakeIsPending() {
    var server = this.registerRemoteNode(NodeServerState.DISCONNECTED, Instant.now().minusSeconds(1));

    this.trackerTask.run();
    this.trackerTask.run();

    Mockito.verify(server, Mockito.times(1)).connect();
  }

  @Test
  void testReconnectIsRepeatedAfterNodeChangedState() {
    var server = this.registerRemoteNode(NodeServerState.DISCONNECTED, Instant.now().minusSeconds(1));
    this.trackerTask.run();

    // the node was marked as disconnected again after the last connection was established
    Mockito.when(server.lastStateChange()).thenReturn(Instant.now().plusMillis(10));
    this.trackerTask.run();

    Mockito.verify(server, Mockito.times(2)).connect();
  }

  @Test
  void testFailedReconnectIsRepeated() {
    var server = this.registerRemoteNode(NodeServerState.DISCONNECTED, Instant.now());
    Mockito.when(server.connect()).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("refused")));

    this.trackerTask.run();
    this.trackerTask.run();

    Mockito.verify(server, Mockito.times(2)).connect();
  }

  @Test
  void testDisconnectedNodeIsRemovedAfterGracePeriod() {
    var lastStateChange = Instant.now().minus(Duration.ofMillis(GRACE_PERIOD_MILLIS + 1_000));
    var server = this.registerRemoteNode(NodeServerState.DISCONNECTED, lastStateChange);

    this.trackerTask.run();

    Mockito.verify(server).close();
    Mockito.verify(server, Mockito.never()).connect();
  }

  @Test
  void testSyncingNodeIsRemovedAfterGracePeriod() {
    var lastStateChange = Instant.now().minus(Duration.ofMillis(GRACE_PERIOD_MILLIS + 1_000));
    var server = this.registerRemoteNode(NodeServerState.SYNCING, lastStateChange);

    this.trackerTask.run();

    Mockito.verify(server).close();
  }

  @Test
  void testSyncingNodeIsNotReconnected() {
    var server = this.registerRemoteNode(NodeServerState.SYNCING, Instant.now());

    this.trackerTask.run();

    Mockito.verify(server, Mockito.never()).connect();
    Mockito.verify(server, Mockito.never()).close();
  }
}
