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

package de.dytanic.cloudnet.provider;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.command.source.DriverCommandSource;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.provider.NodeInfoProvider;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeNodeInfoProvider implements NodeInfoProvider {

  private final IClusterNodeServerProvider clusterNodeServerProvider;

  public NodeNodeInfoProvider(@NotNull CloudNet nodeInstance) {
    this.clusterNodeServerProvider = nodeInstance.getClusterNodeServerProvider();
    nodeInstance.rpcProviderFactory().newHandler(NodeInfoProvider.class, this).registerToDefaultRegistry();
  }

  @Override
  public @NotNull Collection<CommandInfo> consoleCommands() {
    return CloudNet.getInstance().getCommandProvider().getCommands();
  }

  @Override
  public NetworkClusterNode[] nodes() {
    return this.clusterNodeServerProvider.getNodeServers().stream()
      .map(IClusterNodeServer::getNodeInfo)
      .toArray(NetworkClusterNode[]::new);
  }

  @Nullable
  @Override
  public NetworkClusterNode node(@NotNull String uniqueId) {
    Preconditions.checkNotNull(uniqueId);
    // check if the current node is requested
    if (uniqueId.equals(this.clusterNodeServerProvider.getSelfNode().getNodeInfo().uniqueId())) {
      return this.clusterNodeServerProvider.getSelfNode().getNodeInfo();
    }
    // find the node info
    return this.clusterNodeServerProvider.getNodeServers().stream()
      .map(IClusterNodeServer::getNodeInfo)
      .filter(nodeInfo -> nodeInfo.uniqueId().equals(uniqueId))
      .findFirst()
      .orElse(null);
  }

  @Override
  public NetworkClusterNodeInfoSnapshot[] nodeInfoSnapshots() {
    return this.clusterNodeServerProvider.getNodeServers().stream()
      .map(IClusterNodeServer::getNodeInfoSnapshot)
      .filter(Objects::nonNull)
      .toArray(NetworkClusterNodeInfoSnapshot[]::new);
  }

  @Nullable
  @Override
  public NetworkClusterNodeInfoSnapshot nodeInfoSnapshot(@NotNull String uniqueId) {
    // check if the current node is requested
    if (uniqueId.equals(this.clusterNodeServerProvider.getSelfNode().getNodeInfo().uniqueId())) {
      return this.clusterNodeServerProvider.getSelfNode().getNodeInfoSnapshot();
    }
    // find the node we are looking for
    return this.clusterNodeServerProvider.getNodeServers().stream()
      .filter(nodeServer -> nodeServer.getNodeInfo().uniqueId().equals(uniqueId))
      .map(IClusterNodeServer::getNodeInfoSnapshot)
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
  }

  @Override
  public @NotNull Collection<String> sendCommandLine(@NotNull String commandLine) {
    Preconditions.checkNotNull(commandLine);

    var driverCommandSource = new DriverCommandSource();
    CloudNet.getInstance().getCommandProvider().execute(driverCommandSource, commandLine);
    return driverCommandSource.getMessages();
  }

  @Override
  public @NotNull Collection<String> sendCommandLineToNode(@NotNull String nodeUniqueId, @NotNull String commandLine) {
    Preconditions.checkNotNull(nodeUniqueId);
    // check if we should execute the command on the current node
    if (nodeUniqueId.equals(this.clusterNodeServerProvider.getSelfNode().getNodeInfo().uniqueId())) {
      return this.sendCommandLine(commandLine);
    }
    // find the node server and execute the command on there
    var clusterNodeServer = this.clusterNodeServerProvider.getNodeServer(nodeUniqueId);
    if (clusterNodeServer != null && clusterNodeServer.isConnected()) {
      return clusterNodeServer.sendCommandLine(commandLine);
    }
    // unable to execute the command
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public CommandInfo consoleCommand(@NotNull String commandLine) {
    return CloudNet.getInstance().getCommandProvider().getCommand(commandLine);
  }

  @Override
  public @NotNull Collection<String> consoleTabCompleteResults(@NotNull String commandLine) {
    return CloudNet.getInstance().getCommandProvider().suggest(CommandSource.console(), commandLine);
  }
}
