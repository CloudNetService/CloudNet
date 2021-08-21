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

package de.dytanic.cloudnet.wrapper.provider;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.NodeInfoProvider;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrapperNodeInfoProvider implements NodeInfoProvider, DriverAPIUser {

  private final RPCSender rpcSender;
  private final Wrapper wrapper;

  public WrapperNodeInfoProvider(Wrapper wrapper) {
    this.wrapper = wrapper;
    this.rpcSender = wrapper.getRPCProviderFactory()
      .providerForClass(wrapper.getNetworkClient(), NodeInfoProvider.class);
  }

  @Override
  public Collection<CommandInfo> getConsoleCommands() {
    return this.rpcSender.invokeMethod("getConsoleCommands").fireSync();
  }

  @Nullable
  @Override
  public CommandInfo getConsoleCommand(@NotNull String commandLine) {
    Preconditions.checkNotNull(commandLine);
    return this.rpcSender.invokeMethod("getConsoleCommand", commandLine).fireSync();
  }

  @Override
  public Collection<String> getConsoleTabCompleteResults(@NotNull String commandLine) {
    Preconditions.checkNotNull(commandLine);
    return this.rpcSender.invokeMethod("getConsoleTabCompleteResults", commandLine).fireSync();
  }

  @Override
  public String[] sendCommandLine(@NotNull String commandLine) {
    Preconditions.checkNotNull(commandLine);
    return this.rpcSender.invokeMethod("sendCommandLine", commandLine).fireSync();
  }

  @Override
  public String[] sendCommandLine(@NotNull String nodeUniqueId, @NotNull String commandLine) {
    Preconditions.checkNotNull(nodeUniqueId, commandLine);
    return this.rpcSender.invokeMethod("sendCommandLine", nodeUniqueId, commandLine).fireSync();
  }

  @Override
  public NetworkClusterNode[] getNodes() {
    return this.rpcSender.invokeMethod("getNodes").fireSync();
  }

  @Nullable
  @Override
  public NetworkClusterNode getNode(@NotNull String uniqueId) {
    Preconditions.checkNotNull(uniqueId);
    return this.rpcSender.invokeMethod("getNode", uniqueId).fireSync();
  }

  @Override
  public NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots() {
    return this.rpcSender.invokeMethod("getNodeInfoSnapshots").fireSync();
  }

  @Nullable
  @Override
  public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(@NotNull String uniqueId) {
    Preconditions.checkNotNull(uniqueId);
    return this.rpcSender.invokeMethod("getNodeInfoSnapshot", uniqueId).fireSync();
  }

  @Override
  @NotNull
  public ITask<Collection<CommandInfo>> getConsoleCommandsAsync() {
    return CompletableTask.supplyAsync(this::getConsoleCommands);
  }

  @Override
  @NotNull
  public ITask<CommandInfo> getConsoleCommandAsync(@NotNull String commandLine) {
    return CompletableTask.supplyAsync(() -> this.getConsoleCommand(commandLine));
  }

  @Override
  @NotNull
  public ITask<Collection<String>> getConsoleTabCompleteResultsAsync(@NotNull String commandLine) {
    return CompletableTask.supplyAsync(() -> this.getConsoleTabCompleteResults(commandLine));
  }

  @Override
  @NotNull
  public ITask<String[]> sendCommandLineAsync(@NotNull String commandLine) {
    return CompletableTask.supplyAsync(() -> this.sendCommandLine(commandLine));
  }

  @Override
  @NotNull
  public ITask<String[]> sendCommandLineAsync(@NotNull String nodeUniqueId, @NotNull String commandLine) {
    return CompletableTask.supplyAsync(() -> this.sendCommandLine(nodeUniqueId, commandLine));
  }

  @Override
  @NotNull
  public ITask<NetworkClusterNode[]> getNodesAsync() {
    return CompletableTask.supplyAsync(this::getNodes);
  }

  @Override
  @NotNull
  public ITask<NetworkClusterNode> getNodeAsync(@NotNull String uniqueId) {
    return CompletableTask.supplyAsync(() -> this.getNode(uniqueId));
  }

  @Override
  @NotNull
  public ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync() {
    return CompletableTask.supplyAsync(this::getNodeInfoSnapshots);
  }

  @Override
  @NotNull
  public ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(@NotNull String uniqueId) {
    return CompletableTask.supplyAsync(() -> this.getNodeInfoSnapshot(uniqueId));
  }

  @Override
  public INetworkChannel getNetworkChannel() {
    return this.wrapper.getNetworkChannel();
  }
}
