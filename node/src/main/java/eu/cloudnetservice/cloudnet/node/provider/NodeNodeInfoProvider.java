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

import eu.cloudnetservice.cloudnet.driver.command.CommandInfo;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.provider.NodeInfoProvider;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServer;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServerProvider;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.cloudnet.node.command.source.DriverCommandSource;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class NodeNodeInfoProvider implements NodeInfoProvider {

  private final NodeServerProvider clusterNodeServerProvider;

  public NodeNodeInfoProvider(@NonNull CloudNet nodeInstance) {
    this.clusterNodeServerProvider = nodeInstance.nodeServerProvider();
    nodeInstance.rpcProviderFactory().newHandler(NodeInfoProvider.class, this).registerToDefaultRegistry();
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
  public @NonNull Collection<String> sendCommandLineToNode(@NonNull String nodeUniqueId, @NonNull String commandLine) {
    // find the node server and execute the command on there
    var clusterNodeServer = this.clusterNodeServerProvider.node(nodeUniqueId);
    if (clusterNodeServer != null && clusterNodeServer.available()) {
      return clusterNodeServer.sendCommandLine(commandLine);
    }
    // unable to execute the command
    return Collections.emptyList();
  }

  @Override
  public @Nullable CommandInfo consoleCommand(@NonNull String commandLine) {
    return CloudNet.instance().commandProvider().command(commandLine);
  }

  @Override
  public @NonNull Collection<String> consoleTabCompleteResults(@NonNull String commandLine) {
    return CloudNet.instance().commandProvider().suggest(CommandSource.console(), commandLine);
  }
}
