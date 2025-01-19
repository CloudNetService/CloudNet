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

package eu.cloudnetservice.node.impl.cluster.defaults;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.cluster.NodeInfoSnapshot;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.factory.RPCImplementationBuilder;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.impl.cluster.util.NodeDisconnectHandler;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class RemoteNodeServer implements NodeServer {

  private static final Type COLLECTION_STRING = TypeFactory.parameterizedClass(Set.class, String.class);

  private final NetworkClient networkClient;
  private final DataSyncRegistry dataSyncRegistry;
  private final NodeDisconnectHandler disconnectHandler;
  private final CloudServiceProvider cloudServiceProvider;

  private final NetworkClusterNode info;
  private final NodeServerProvider provider;
  private final CloudServiceFactory serviceFactory;

  private volatile NetworkChannel channel;
  private volatile Instant lastStateChange = Instant.now();
  private volatile NodeServerState state = NodeServerState.UNAVAILABLE;

  private volatile NodeInfoSnapshot currentSnapshot;
  private volatile NodeInfoSnapshot lastSnapshot;
  private volatile Instant lastNodeInfoUpdate = Instant.now();

  @Inject
  public RemoteNodeServer(
    @NonNull NetworkClusterNode info,
    @NonNull NodeServerProvider provider,
    @NonNull NetworkClient networkClient,
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull NodeDisconnectHandler disconnectHandler,
    @NonNull CloudServiceProvider cloudServiceProvider,
    @NonNull RPCImplementationBuilder.InstanceAllocator<CloudServiceFactory> serviceFactoryAllocator
  ) {
    this.info = info;
    this.provider = provider;
    this.networkClient = networkClient;
    this.dataSyncRegistry = dataSyncRegistry;
    this.disconnectHandler = disconnectHandler;
    this.cloudServiceProvider = cloudServiceProvider;
    this.serviceFactory = serviceFactoryAllocator.withTargetChannel(this::channel).allocate();
  }

  @Override
  public @NonNull String name() {
    return this.info.uniqueId();
  }

  @Override
  public boolean head() {
    return this.provider.headNode().equals(this);
  }

  @Override
  public boolean available() {
    return this.channel != null && this.currentSnapshot != null && this.state == NodeServerState.READY;
  }

  @Override
  public void shutdown() {
    ChannelMessage.builder()
      .sendSync(true)
      .message("cluster_node_shutdown")
      .targetNode(this.info.uniqueId())
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .build()
      .send();
  }

  @Override
  public @NonNull CompletableFuture<Void> connect() {
    // check if the node has any listeners
    var listeners = this.info.listeners();
    if (listeners.isEmpty()) {
      return CompletableFuture.failedFuture(new IllegalStateException("No listeners registered for the node"));
    }
    // select a random listener and try to connect to it
    var listener = listeners.get(ThreadLocalRandom.current().nextInt(0, listeners.size()));
    return this.networkClient.connect(listener);
  }

  @Override
  public boolean draining() {
    return this.currentSnapshot != null && this.currentSnapshot.draining();
  }

  @Override
  public void drain(boolean doDrain) {
    ChannelMessage.builder()
      .message("change_draining_state")
      .targetNode(this.info.uniqueId())
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeBoolean(doDrain))
      .build()
      .send();
  }

  @Override
  public void syncClusterData(boolean force) {
    ChannelMessage.builder()
      .message("sync_cluster_data")
      .targetNode(this.info.uniqueId())
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(this.dataSyncRegistry.prepareClusterData(force))
      .build()
      .send();
  }

  @Override
  public @NonNull NetworkClusterNode info() {
    return this.info;
  }

  @Override
  public @NonNull NodeServerProvider provider() {
    return this.provider;
  }

  @Override
  public @NonNull NodeServerState state() {
    return this.state;
  }

  @Override
  public void state(@NonNull NodeServerState state) {
    this.state = state;
    this.lastStateChange = Instant.now();
  }

  @Override
  public @NonNull Instant lastStateChange() {
    return this.lastStateChange;
  }

  @Override
  public @UnknownNullability NetworkChannel channel() {
    return this.channel;
  }

  @Override
  public void channel(@Nullable NetworkChannel channel) {
    this.channel = channel;
  }

  @Override
  public @UnknownNullability NodeInfoSnapshot nodeInfoSnapshot() {
    return this.currentSnapshot;
  }

  @Override
  public @UnknownNullability NodeInfoSnapshot lastNodeInfoSnapshot() {
    return this.lastSnapshot;
  }

  @Override
  public void updateNodeInfoSnapshot(@Nullable NodeInfoSnapshot snapshot) {
    if (snapshot == null) {
      // reset the snapshot, for example a disconnect
      this.lastSnapshot = null;
      this.currentSnapshot = null;
    } else if (this.currentSnapshot == null) {
      // no snapshot is available, first time connection
      this.currentSnapshot = this.lastSnapshot = snapshot;
      this.provider.selectHeadNode();
    } else {
      // pre-move the current snapshot to the last snapshot
      this.lastSnapshot = this.currentSnapshot;
      this.currentSnapshot = snapshot;
    }

    // mark the last info update time
    this.lastNodeInfoUpdate = Instant.now();
  }

  @Override
  public @NonNull Instant lastNodeInfoUpdate() {
    return this.lastNodeInfoUpdate;
  }

  @Override
  public @NonNull CloudServiceFactory serviceFactory() {
    return this.serviceFactory;
  }

  @Override
  public @Nullable SpecificCloudServiceProvider serviceProvider(@NonNull UUID uniqueId) {
    return this.cloudServiceProvider.serviceProvider(uniqueId);
  }

  @Override
  public @NonNull Collection<String> sendCommandLine(@NonNull String commandLine) {
    return ChannelMessage.builder()
      .message("send_command_line")
      .targetNode(this.info.uniqueId())
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeString(commandLine))
      .build()
      .sendSingleQueryAsync()
      .thenApply(message -> message.content().<Collection<String>>readObject(COLLECTION_STRING))
      .exceptionally($ -> Set.of())
      .join();
  }

  @Override
  public void close() {
    // disconnect the node from the network
    if (this.channel != null) {
      this.channel.close();
      this.channel = null;
    }
    // reset the node info snapshot & mark as removed
    this.updateNodeInfoSnapshot(null);
    this.state(NodeServerState.UNAVAILABLE);
    // notify about the service remove from that node, then trigger a head node refresh
    this.disconnectHandler.handleNodeServerClose(this);
    this.provider.selectHeadNode();
  }
}
