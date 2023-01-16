/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.cluster.defaults;

import dev.derklaro.aerogel.PostConstruct;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.network.cluster.NetworkCluster;
import eu.cloudnetservice.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.cluster.LocalNodeServer;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.cluster.task.LocalNodeUpdateTask;
import eu.cloudnetservice.node.cluster.task.NodeDisconnectTrackerTask;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@Provides(NodeServerProvider.class)
public class DefaultNodeServerProvider implements NodeServerProvider {

  private final DataSyncRegistry dataSyncRegistry;

  private final LocalNodeServer localNode;
  private final Collection<NodeServer> nodeServers;

  private final LocalNodeUpdateTask localNodeUpdateTask;
  private final NodeDisconnectTrackerTask disconnectTrackerTask;

  // this executor handles everything which is needed for the cluster to work properly
  // as we normally scheduled 2 tasks (1 to send a local node update regularly, 1 to keep track of node disconnects), the
  // core pool size is set to 2
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

  private volatile NodeServer headNode;

  @Inject
  public DefaultNodeServerProvider(
    @NonNull LocalNodeServer localNode,
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull LocalNodeUpdateTask localNodeUpdateTask,
    @NonNull NodeDisconnectTrackerTask disconnectTrackerTask
  ) {
    this.dataSyncRegistry = dataSyncRegistry;
    this.localNodeUpdateTask = localNodeUpdateTask;
    this.disconnectTrackerTask = disconnectTrackerTask;

    // create and register the local node server
    this.localNode = localNode;
    this.nodeServers = new HashSet<>();
  }

  @PostConstruct
  private void finishConstruction() {
    // register the local node server
    this.nodeServers.add(this.localNode);

    // start all update tasks
    this.executor.scheduleAtFixedRate(this.localNodeUpdateTask, 1, 1, TimeUnit.SECONDS);
    this.executor.scheduleAtFixedRate(this.disconnectTrackerTask, 5, 5, TimeUnit.SECONDS);
  }

  @Override
  public @NonNull Collection<NodeServer> nodeServers() {
    return Collections.unmodifiableCollection(this.nodeServers);
  }

  @Override
  public @NonNull Collection<NodeServer> availableNodeServers() {
    return this.nodeServers.stream().filter(NodeServer::available).toList();
  }

  @Override
  public @NonNull Collection<NetworkChannel> connectedNodeChannels() {
    return this.nodeServers.stream().map(NodeServer::channel).filter(Objects::nonNull).toList();
  }

  @Override
  public @NonNull NodeServer headNode() {
    return this.headNode;
  }

  @Override
  public @NonNull LocalNodeServer localNode() {
    return this.localNode;
  }

  @Override
  public @Nullable NodeServer node(@NonNull String uniqueId) {
    return this.nodeServers.stream().filter(server -> server.name().equals(uniqueId)).findFirst().orElse(null);
  }

  @Override
  public @Nullable NodeServer node(@NonNull NetworkChannel channel) {
    return this.nodeServers.stream()
      .filter(server -> server.channel() != null && channel.equals(server.channel()))
      .findFirst()
      .orElse(null);
  }

  @Override
  public void syncDataIntoCluster() {
    ChannelMessage.builder()
      .targetNodes()
      .message("sync_cluster_data")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(this.dataSyncRegistry.prepareClusterData(true))
      .build()
      .send();
  }

  @Override
  public void registerNodes(@NonNull NetworkCluster cluster) {
    // remove all remote node servers
    this.nodeServers.removeIf(server -> !(server instanceof LocalNodeServer));
    cluster.nodes().forEach(this::registerNode);
  }

  @Override
  public void registerNode(@NonNull NetworkClusterNode clusterNode) {
    var server = InjectionLayer.boot().instance(
      RemoteNodeServer.class,
      builder -> builder.override(NetworkClusterNode.class, clusterNode));
    this.nodeServers.add(server);
  }

  @Override
  public void unregisterNode(@NonNull String uniqueId) {
    this.nodeServers.stream()
      .filter(server -> !(server instanceof LocalNodeServer))
      .filter(server -> server.name().equals(uniqueId))
      .findFirst()
      .ifPresent(server -> {
        server.close();
        this.nodeServers.remove(server);
      });
  }

  @Override
  public void selectHeadNode() {
    this.headNode = this.nodeServers.stream()
      .filter(NodeServer::available)
      .min(Comparator.comparingLong(nodeServer -> nodeServer.nodeInfoSnapshot().startupMillis()))
      .orElseThrow();
  }

  @Override
  public @NonNull Task<TransferStatus> deployTemplateToCluster(
    @NonNull ServiceTemplate template,
    @NonNull InputStream stream,
    boolean overwrite
  ) {
    // collect all known & available channels in the cluster
    var channels = this.connectedNodeChannels();
    if (!channels.isEmpty()) {
      // send the template chunked to the cluster
      return ChunkedPacketSender.forFileTransfer()
        .transferChannel("deploy_service_template")
        .withExtraData(
          DataBuf.empty().writeString(template.storageName()).writeObject(template).writeBoolean(overwrite))
        .toChannels(channels)
        .source(stream)
        .build()
        .transferChunkedData();
    }
    // if there are no channels we "pseudo" completed the transfer
    return Task.completedTask(TransferStatus.SUCCESS);
  }

  @Override
  public @NonNull Task<TransferStatus> deployStaticServiceToCluster(
    @NonNull String name,
    @NonNull InputStream stream,
    boolean overwrite
  ) {
    // collect all known & available channels in the cluster
    var channels = this.connectedNodeChannels();
    if (!channels.isEmpty()) {
      // send the template chunked to the cluster
      return ChunkedPacketSender.forFileTransfer()
        .transferChannel("deploy_static_service")
        .withExtraData(DataBuf.empty().writeString(name).writeBoolean(overwrite))
        .toChannels(channels)
        .source(stream)
        .build()
        .transferChunkedData();
    }
    // if there are no channels we "pseudo" completed the transfer
    return Task.completedTask(TransferStatus.SUCCESS);
  }

  @Override
  public void sendPacket(@NonNull Packet packet) {
    for (var server : this.nodeServers) {
      var channel = server.channel();
      if (channel != null) {
        channel.sendPacket(packet);
      }
    }
  }

  @Override
  public void sendPacketSync(@NonNull Packet packet) {
    for (var server : this.nodeServers) {
      var channel = server.channel();
      if (channel != null) {
        channel.sendPacketSync(packet);
      }
    }
  }

  @Override
  public void close() {
    this.nodeServers.forEach(server -> {
      // do not close the local node server
      if (!(server instanceof LocalNodeServer)) {
        server.close();
      }
    });
    // re-select the head node in case some api stored an instance of this class
    this.selectHeadNode();
    this.executor.shutdownNow();
  }
}
