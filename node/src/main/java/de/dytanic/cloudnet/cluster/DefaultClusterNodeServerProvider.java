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

package de.dytanic.cloudnet.cluster;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketSender;
import de.dytanic.cloudnet.driver.network.chunk.TransferStatus;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.network.listener.message.NodeChannelMessageListener;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public final class DefaultClusterNodeServerProvider extends DefaultNodeServerProvider<IClusterNodeServer>
  implements IClusterNodeServerProvider {

  private static final Logger LOGGER = LogManager.logger(DefaultClusterNodeServerProvider.class);

  private static final Format TIME_FORMAT = new DecimalFormat("##.###");
  private static final long MAX_NO_UPDATE_MILLIS = Long.getLong("cloudnet.max.node.idle.millis", 30_000);

  public DefaultClusterNodeServerProvider(@NonNull CloudNet cloudNet) {
    super(cloudNet);

    // register the event for channel message handling
    cloudNet.eventManager().registerListener(new NodeChannelMessageListener(
      cloudNet.eventManager(),
      cloudNet.dataSyncRegistry(),
      this));
    // schedule the task for updating of the local node information
    cloudNet.taskExecutor().scheduleAtFixedRate(() -> {
      if (this.localNode.available()) {
        try {
          this.checkForDeadNodes();
          this.localNode.publishNodeInfoSnapshotUpdate();
        } catch (Throwable throwable) {
          LOGGER.severe("Exception while ticking node server provider", throwable);
        }
      }
    }, 1, 1, TimeUnit.SECONDS);
  }

  @Override
  public @Nullable IClusterNodeServer nodeServer(@NonNull INetworkChannel channel) {
    Preconditions.checkNotNull(channel);

    for (var clusterNodeServer : this.nodeServers()) {
      if (clusterNodeServer.channel() != null
        && clusterNodeServer.channel().channelId() == channel.channelId()) {
        return clusterNodeServer;
      }
    }

    return null;
  }

  @Override
  public void clusterServers(@NonNull NetworkCluster networkCluster) {
    for (var clusterNode : networkCluster.nodes()) {
      NodeServer nodeServer = this.nodeServer(clusterNode.uniqueId());
      if (nodeServer != null) {
        nodeServer.nodeInfo(clusterNode);
      } else {
        this.nodeServers.add(new DefaultClusterNodeServer(this.cloudNet, this, clusterNode));
      }
    }

    for (var clusterNodeServer : this.nodeServers) {
      var node = networkCluster.nodes()
        .stream()
        .filter(cluNode -> cluNode.uniqueId().equalsIgnoreCase(clusterNodeServer.nodeInfo().uniqueId()))
        .findFirst()
        .orElse(null);
      if (node == null) {
        this.nodeServers.removeIf(
          nodeServer -> nodeServer.nodeInfo().uniqueId().equals(clusterNodeServer.nodeInfo().uniqueId()));
      }
    }
  }

  @Override
  public void sendPacket(@NonNull IPacket packet) {
    Preconditions.checkNotNull(packet);

    for (var nodeServer : this.nodeServers) {
      nodeServer.saveSendPacket(packet);
    }
  }

  @Override
  public void sendPacketSync(@NonNull IPacket packet) {
    Preconditions.checkNotNull(packet);

    for (var nodeServer : this.nodeServers) {
      nodeServer.saveSendPacketSync(packet);
    }
  }

  @Override
  public @NonNull ITask<TransferStatus> deployTemplateToCluster(
    @NonNull ServiceTemplate template,
    @NonNull InputStream stream,
    boolean overwrite
  ) {
    // collect all known & available channels in the cluster
    var channels = this.collectClusterChannel();
    // check if there is a channel to deploy to
    if (!channels.isEmpty()) {
      // send the template chunked to the cluster
      return ChunkedPacketSender.forFileTransfer()
        .transferChannel("deploy_service_template")
        .withExtraData(DataBuf.empty().writeString(template.storageName()).writeObject(template).writeBoolean(overwrite))
        .toChannels(channels)
        .source(stream)
        .build()
        .transferChunkedData();
    }
    // always successful if there is no node to deploy to
    return CompletedTask.done(TransferStatus.SUCCESS);
  }

  @Override
  public @NonNull ITask<TransferStatus> deployStaticServiceToCluster(
    @NonNull String name,
    @NonNull InputStream stream,
    boolean overwrite
  ) {
    // collect all known & available channels in the cluster
    var channels = this.collectClusterChannel();
    // check if there is a channel to deploy to
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
    return CompletedTask.done(TransferStatus.SUCCESS);
  }

  @Override
  @UnmodifiableView
  public @NonNull Collection<INetworkChannel> connectedChannels() {
    return this.nodeServers().stream()
      .map(IClusterNodeServer::channel)
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public boolean hasAnyConnection() {
    var servers = this.nodeServers();
    return !servers.isEmpty() && servers.stream().anyMatch(IClusterNodeServer::connected);
  }

  @Override
  public void checkForDeadNodes() {
    for (var nodeServer : this.nodeServers) {
      if (nodeServer.available()) {
        var snapshot = nodeServer.nodeInfoSnapshot();
        if (snapshot != null && snapshot.creationTime() + MAX_NO_UPDATE_MILLIS < System.currentTimeMillis()) {
          try {
            LOGGER.info(I18n.trans("cluster-server-idling-too-long")
              .replace("%id%", nodeServer.nodeInfo().uniqueId())
              .replace("%time%", TIME_FORMAT.format((System.currentTimeMillis() - snapshot.creationTime()) / 1000)));
            nodeServer.close();
          } catch (Exception exception) {
            LOGGER.severe("Exception while closing server", exception);
          }
        }
      }
    }
  }

  @Override
  public void syncClusterData() {
    ChannelMessage.builder()
      .targetNodes()
      .message("sync_cluster_data")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(this.cloudNet.dataSyncRegistry().prepareClusterData(true))
      .build()
      .send();
  }

  @Override
  public void close() throws Exception {
    for (var clusterNodeServer : this.nodeServers) {
      clusterNodeServer.close();
    }

    this.nodeServers.clear();
    this.refreshHeadNode();
  }

  private @NonNull Collection<INetworkChannel> collectClusterChannel() {
    if (!this.nodeServers.isEmpty()) {
      // collect the network channels of the connected nodes
      return this.nodeServers
        .stream()
        .filter(IClusterNodeServer::available)
        .map(IClusterNodeServer::channel)
        .filter(Objects::nonNull)
        .toList();
    } else {
      return Collections.emptyList();
    }
  }
}
