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
import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.DriverCommandSender;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.provider.NodeInfoProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeNodeInfoProvider implements NodeInfoProvider {

  private final CloudNet cloudNet;

  public NodeNodeInfoProvider(CloudNet cloudNet) {
    this.cloudNet = cloudNet;
  }

  @Override
  public Collection<CommandInfo> getConsoleCommands() {
    return this.cloudNet.getCommandMap().getCommandInfos();
  }

  @Override
  public NetworkClusterNode[] getNodes() {
    return this.cloudNet.getConfig().getClusterConfig().getNodes().toArray(new NetworkClusterNode[0]);
  }

  @Nullable
  @Override
  public NetworkClusterNode getNode(@NotNull String uniqueId) {
    Preconditions.checkNotNull(uniqueId);

    if (uniqueId.equals(this.cloudNet.getConfig().getIdentity().getUniqueId())) {
      return this.cloudNet.getConfig().getIdentity();
    }
    return this.cloudNet.getConfig().getClusterConfig().getNodes().stream()
      .filter(networkClusterNode -> networkClusterNode.getUniqueId().equals(uniqueId))
      .findFirst()
      .orElse(null);
  }

  @Override
  public NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots() {
    Collection<NetworkClusterNodeInfoSnapshot> nodeInfoSnapshots = new ArrayList<>();

    for (IClusterNodeServer clusterNodeServer : this.cloudNet.getClusterNodeServerProvider().getNodeServers()) {
      if (clusterNodeServer.isConnected()) {
        if (clusterNodeServer.getNodeInfoSnapshot() != null) {
          nodeInfoSnapshots.add(clusterNodeServer.getNodeInfoSnapshot());
        }
      }
    }

    return nodeInfoSnapshots.toArray(new NetworkClusterNodeInfoSnapshot[0]);
  }

  @Nullable
  @Override
  public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(@NotNull String uniqueId) {
    if (uniqueId.equals(this.cloudNet.getConfig().getIdentity().getUniqueId())) {
      return this.cloudNet.getCurrentNetworkClusterNodeInfoSnapshot();
    }

    for (IClusterNodeServer clusterNodeServer : this.cloudNet.getClusterNodeServerProvider().getNodeServers()) {
      if (clusterNodeServer.getNodeInfo().getUniqueId().equals(uniqueId) && clusterNodeServer.isConnected()) {
        if (clusterNodeServer.getNodeInfoSnapshot() != null) {
          return clusterNodeServer.getNodeInfoSnapshot();
        }
      }
    }

    return null;
  }

  @Override
  public String[] sendCommandLine(@NotNull String commandLine) {
    Preconditions.checkNotNull(commandLine);

    Collection<String> collection = new ArrayList<>();

    if (this.cloudNet.isMainThread()) {
      this.sendCommandLine0(collection, commandLine);
    } else {
      try {
        this.cloudNet.runTask((Callable<Void>) () -> {
          this.sendCommandLine0(collection, commandLine);
          return null;
        }).get();
      } catch (InterruptedException | ExecutionException exception) {
        exception.printStackTrace();
      }
    }

    return collection.toArray(new String[0]);
  }

  @Override
  public String[] sendCommandLine(@NotNull String nodeUniqueId, @NotNull String commandLine) {
    Preconditions.checkNotNull(nodeUniqueId);
    Preconditions.checkNotNull(commandLine);

    if (this.cloudNet.getConfig().getIdentity().getUniqueId().equals(nodeUniqueId)) {
      return this.sendCommandLine(commandLine);
    }

    IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(nodeUniqueId);

    if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
      return clusterNodeServer.sendCommandLine(commandLine);
    }

    return null;
  }

  private void sendCommandLine0(Collection<String> collection, String commandLine) {
    this.cloudNet.getCommandMap().dispatchCommand(new DriverCommandSender(collection), commandLine);
  }

  @Nullable
  @Override
  public CommandInfo getConsoleCommand(@NotNull String commandLine) {
    Command command = this.cloudNet.getCommandMap().getCommandFromLine(commandLine);
    return command != null ? command.getInfo() : null;
  }

  @Override
  public Collection<String> getConsoleTabCompleteResults(@NotNull String commandLine) {
    return this.cloudNet.getCommandMap().tabCompleteCommand(commandLine);
  }

  @Override
  @NotNull
  public ITask<Collection<CommandInfo>> getConsoleCommandsAsync() {
    return this.cloudNet.scheduleTask(this::getConsoleCommands);
  }

  @Override
  @NotNull
  public ITask<CommandInfo> getConsoleCommandAsync(@NotNull String commandLine) {
    return this.cloudNet.scheduleTask(() -> this.getConsoleCommand(commandLine));
  }

  @Override
  @NotNull
  public ITask<Collection<String>> getConsoleTabCompleteResultsAsync(@NotNull String commandLine) {
    return this.cloudNet.scheduleTask(() -> this.getConsoleTabCompleteResults(commandLine));
  }

  @Override
  @NotNull
  public ITask<String[]> sendCommandLineAsync(@NotNull String commandLine) {
    return this.cloudNet.scheduleTask(() -> this.sendCommandLine(commandLine));
  }

  @Override
  @NotNull
  public ITask<String[]> sendCommandLineAsync(@NotNull String nodeUniqueId, @NotNull String commandLine) {
    return this.cloudNet.scheduleTask(() -> this.sendCommandLine(nodeUniqueId, commandLine));
  }

  @Override
  @NotNull
  public ITask<NetworkClusterNode[]> getNodesAsync() {
    return this.cloudNet.scheduleTask(this::getNodes);
  }

  @Override
  @NotNull
  public ITask<NetworkClusterNode> getNodeAsync(@NotNull String uniqueId) {
    Preconditions.checkNotNull(uniqueId);

    return this.cloudNet.scheduleTask(() -> this.getNode(uniqueId));
  }

  @Override
  @NotNull
  public ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync() {
    return this.cloudNet.scheduleTask(this::getNodeInfoSnapshots);
  }

  @Override
  @NotNull
  public ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(@NotNull String uniqueId) {
    Preconditions.checkNotNull(uniqueId);

    return this.cloudNet.scheduleTask(() -> this.getNodeInfoSnapshot(uniqueId));
  }
}
