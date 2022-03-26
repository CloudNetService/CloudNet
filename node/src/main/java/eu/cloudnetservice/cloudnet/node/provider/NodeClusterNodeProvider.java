/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.provider;

import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.command.CommandInfo;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServer;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServerProvider;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.cloudnet.node.command.source.DriverCommandSource;
import eu.cloudnetservice.cloudnet.node.network.listener.message.NodeChannelMessageListener;
import java.util.Collection;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class NodeClusterNodeProvider implements ClusterNodeProvider {

  private final CloudNet nodeInstance;
  private final NodeServerProvider clusterNodeServerProvider;

  public NodeClusterNodeProvider(@NonNull CloudNet nodeInstance) {
    this.nodeInstance = nodeInstance;
    this.clusterNodeServerProvider = nodeInstance.nodeServerProvider();

    // init
    nodeInstance.eventManager().registerListener(new NodeChannelMessageListener(
      nodeInstance.eventManager(),
      nodeInstance.dataSyncRegistry(),
      this,
      nodeInstance.nodeServerProvider()));
    nodeInstance.rpcFactory().newHandler(ClusterNodeProvider.class, this).registerToDefaultRegistry();
  }

  @Override
  public @NonNull Collection<CommandInfo> consoleCommands() {
    return CloudNet.instance().commandProvider().commands();
  }

  @Override
  public @NonNull Collection<NetworkClusterNode> nodes() {
    return this.clusterNodeServerProvider.nodeServers().stream().map(NodeServer::info).toList();
  }

  @Override
  public @Nullable NetworkClusterNode node(@NonNull String uniqueId) {
    // find the node info
    return this.clusterNodeServerProvider.nodeServers().stream()
      .map(NodeServer::info)
      .filter(nodeInfo -> nodeInfo.uniqueId().equals(uniqueId))
      .findFirst()
      .orElse(null);
  }

  @Override
  public boolean addNode(@NonNull NetworkClusterNode node) {
    // prevent duplicate node registrations
    if (this.node(node.uniqueId()) == null) {
      this.addNodeSilently(node);
      // send the update to all nodes
      ChannelMessage.builder()
        .targetNodes()
        .message("register_known_node")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .buffer(DataBuf.empty().writeObject(node))
        .build()
        .send();
      return true;
    }
    // the node is already present
    return false;
  }

  @Override
  public boolean removeNode(@NonNull String uniqueId) {
    // check if the node is still registered
    var clusterNode = this.node(uniqueId);
    if (clusterNode != null) {
      this.removeNodeSilently(clusterNode);
      // send the update to all nodes
      ChannelMessage.builder()
        .targetNodes()
        .message("remove_known_node")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .buffer(DataBuf.empty().writeObject(clusterNode))
        .build()
        .send();
      return true;
    }
    // the node is not present
    return false;
  }

  @Override
  public @NonNull Collection<NetworkClusterNodeInfoSnapshot> nodeInfoSnapshots() {
    return this.clusterNodeServerProvider.nodeServers().stream()
      .map(NodeServer::nodeInfoSnapshot)
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public @Nullable NetworkClusterNodeInfoSnapshot nodeInfoSnapshot(@NonNull String uniqueId) {
    // find the node we are looking for
    return this.clusterNodeServerProvider.nodeServers().stream()
      .filter(nodeServer -> nodeServer.info().uniqueId().equals(uniqueId))
      .map(NodeServer::nodeInfoSnapshot)
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
  }

  @Override
  public @NonNull Collection<String> sendCommandLine(@NonNull String commandLine) {
    var driverCommandSource = new DriverCommandSource();
    CloudNet.instance().commandProvider().execute(driverCommandSource, commandLine).getOrNull();
    return driverCommandSource.messages();
  }

  @Override
  public @Nullable CommandInfo consoleCommand(@NonNull String commandLine) {
    return CloudNet.instance().commandProvider().command(commandLine);
  }

  @Override
  public @NonNull Collection<String> consoleTabCompleteResults(@NonNull String commandLine) {
    return CloudNet.instance().commandProvider().suggest(CommandSource.console(), commandLine);
  }

  public void addNodeSilently(@NonNull NetworkClusterNode node) {
    // register the node
    var config = this.nodeInstance.config();
    config.clusterConfig().nodes().add(node);
    // register all hosts
    node.listeners().forEach(hostAndPort -> config.ipWhitelist().add(hostAndPort.host()));
    config.save();

    // register the node to the provider
    this.clusterNodeServerProvider.registerNode(node);
  }

  public void removeNodeSilently(@NonNull NetworkClusterNode node) {
    // unregister the node
    var config = this.nodeInstance.config();
    config.clusterConfig().nodes().remove(node);
    // unregister all hosts
    node.listeners().forEach(hostAndPort -> config.ipWhitelist().remove(hostAndPort.host()));
    config.save();

    // unregister the node from the provider
    this.clusterNodeServerProvider.unregisterNode(node.uniqueId());
  }
}
