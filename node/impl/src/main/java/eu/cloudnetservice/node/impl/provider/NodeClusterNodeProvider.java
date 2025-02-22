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

package eu.cloudnetservice.node.impl.provider;

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.cluster.NodeInfoSnapshot;
import eu.cloudnetservice.driver.command.CommandInfo;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.command.source.DriverCommandSource;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@Provides(ClusterNodeProvider.class)
public class NodeClusterNodeProvider implements ClusterNodeProvider {

  private final Configuration configuration;
  private final CommandProvider commandProvider;
  private final NodeServerProvider clusterNodeServerProvider;

  @Inject
  public NodeClusterNodeProvider(
    @NonNull Configuration configuration,
    @NonNull CommandProvider commandProvider,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull RPCFactory rpcFactory,
    @NonNull RPCHandlerRegistry handlerRegistry
  ) {
    this.configuration = configuration;
    this.commandProvider = commandProvider;
    this.clusterNodeServerProvider = nodeServerProvider;

    // add the rpc handler
    var rpcHandler = rpcFactory.newRPCHandlerBuilder(ClusterNodeProvider.class).targetInstance(this).build();
    handlerRegistry.registerHandler(rpcHandler);
  }

  @Override
  public @NonNull Collection<CommandInfo> consoleCommands() {
    return this.commandProvider.commands();
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
  public @NonNull Collection<NodeInfoSnapshot> nodeInfoSnapshots() {
    return this.clusterNodeServerProvider.nodeServers().stream()
      .map(NodeServer::nodeInfoSnapshot)
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public @Nullable NodeInfoSnapshot nodeInfoSnapshot(@NonNull String uniqueId) {
    // find the node we are looking for
    return this.clusterNodeServerProvider.nodeServers().stream()
      .filter(nodeServer -> nodeServer.info().uniqueId().equals(uniqueId))
      .map(NodeServer::nodeInfoSnapshot)
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
  }

  @Override
  public @NonNull CompletableFuture<Collection<CommandInfo>> consoleCommandsAsync() {
    return TaskUtil.supplyAsync(this::consoleCommands);
  }

  @Override
  public @NonNull CompletableFuture<CommandInfo> consoleCommandAsync(@NonNull String name) {
    return TaskUtil.supplyAsync(() -> this.consoleCommand(name));
  }

  @Override
  public @NonNull CompletableFuture<Collection<String>> consoleTabCompleteResultsAsync(@NonNull String commandLine) {
    return TaskUtil.supplyAsync(() -> this.consoleTabCompleteResults(commandLine));
  }

  @Override
  public @NonNull CompletableFuture<Collection<String>> sendCommandLineAsync(@NonNull String commandLine) {
    return TaskUtil.supplyAsync(() -> this.sendCommandLine(commandLine));
  }

  @Override
  public @NonNull CompletableFuture<Collection<NetworkClusterNode>> nodesAsync() {
    return TaskUtil.supplyAsync(this::nodes);
  }

  @Override
  public @NonNull CompletableFuture<NetworkClusterNode> nodeAsync(@NonNull String uniqueId) {
    return TaskUtil.supplyAsync(() -> this.node(uniqueId));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> addNodeAsync(@NonNull NetworkClusterNode node) {
    return TaskUtil.supplyAsync(() -> this.addNode(node));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> removeNodeAsync(@NonNull String uniqueId) {
    return TaskUtil.supplyAsync(() -> this.removeNode(uniqueId));
  }

  @Override
  public @NonNull CompletableFuture<Collection<NodeInfoSnapshot>> nodeInfoSnapshotsAsync() {
    return TaskUtil.supplyAsync(this::nodeInfoSnapshots);
  }

  @Override
  public @NonNull CompletableFuture<NodeInfoSnapshot> nodeInfoSnapshotAsync(@NonNull String uniqueId) {
    return TaskUtil.supplyAsync(() -> this.nodeInfoSnapshot(uniqueId));
  }

  @Override
  public @NonNull Collection<String> sendCommandLine(@NonNull String commandLine) {
    var driverCommandSource = new DriverCommandSource();
    TaskUtil.getOrDefault(this.commandProvider.execute(driverCommandSource, commandLine), null);
    return driverCommandSource.messages();
  }

  @Override
  public @Nullable CommandInfo consoleCommand(@NonNull String name) {
    return this.commandProvider.command(name);
  }

  @Override
  public @NonNull Collection<String> consoleTabCompleteResults(@NonNull String commandLine) {
    return this.commandProvider.suggest(CommandSource.console(), commandLine);
  }

  public void addNodeSilently(@NonNull NetworkClusterNode node) {
    // register the node
    this.configuration.clusterConfig().nodes().add(node);
    // register all hosts
    node.listeners().forEach(hostAndPort -> this.configuration.ipWhitelist().add(hostAndPort.host()));
    this.configuration.save();

    // register the node to the provider
    this.clusterNodeServerProvider.registerNode(node);
  }

  public void removeNodeSilently(@NonNull NetworkClusterNode node) {
    // unregister the node
    this.configuration.clusterConfig().nodes().remove(node);
    // unregister all hosts
    node.listeners().forEach(hostAndPort -> this.configuration.ipWhitelist().remove(hostAndPort.host()));
    this.configuration.save();

    // unregister the node from the provider
    this.clusterNodeServerProvider.unregisterNode(node.uniqueId());
  }
}
