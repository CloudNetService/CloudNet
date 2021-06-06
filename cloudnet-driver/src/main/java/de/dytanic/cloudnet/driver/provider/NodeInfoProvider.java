package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NodeInfoProvider {

  Collection<CommandInfo> getConsoleCommands();

  @Nullable
  CommandInfo getConsoleCommand(@NotNull String commandLine);

  Collection<String> getConsoleTabCompleteResults(@NotNull String commandLine);

  String[] sendCommandLine(@NotNull String commandLine);

  String[] sendCommandLine(@NotNull String nodeUniqueId, @NotNull String commandLine);

  @NotNull
  ITask<Collection<CommandInfo>> getConsoleCommandsAsync();

  @NotNull
  ITask<CommandInfo> getConsoleCommandAsync(@NotNull String commandLine);

  @NotNull
  ITask<Collection<String>> getConsoleTabCompleteResultsAsync(@NotNull String commandLine);

  @NotNull
  ITask<String[]> sendCommandLineAsync(@NotNull String commandLine);

  @NotNull
  ITask<String[]> sendCommandLineAsync(@NotNull String nodeUniqueId, @NotNull String commandLine);

  @NotNull
  ITask<NetworkClusterNode[]> getNodesAsync();

  @NotNull
  ITask<NetworkClusterNode> getNodeAsync(@NotNull String uniqueId);

  @NotNull
  ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync();

  @NotNull
  ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(@NotNull String uniqueId);

  NetworkClusterNode[] getNodes();

  @Nullable
  NetworkClusterNode getNode(@NotNull String uniqueId);

  NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots();

  @Nullable
  NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(@NotNull String uniqueId);

}
