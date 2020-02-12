package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.command.CommandInfo;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface NodeInfoProvider {

    Collection<CommandInfo> getConsoleCommands();

    @Nullable
    CommandInfo getConsoleCommand(@NotNull String commandLine);

    Collection<String> getConsoleTabCompleteResults(@NotNull String commandLine);

    String[] sendCommandLine(@NotNull String commandLine);

    String[] sendCommandLine(@NotNull String nodeUniqueId, @NotNull String commandLine);

    ITask<Collection<CommandInfo>> getConsoleCommandsAsync();

    ITask<CommandInfo> getConsoleCommandAsync(@NotNull String commandLine);

    ITask<Collection<String>> getConsoleTabCompleteResultsAsync(@NotNull String commandLine);

    ITask<String[]> sendCommandLineAsync(@NotNull String commandLine);

    ITask<String[]> sendCommandLineAsync(@NotNull String nodeUniqueId, @NotNull String commandLine);

    ITask<NetworkClusterNode[]> getNodesAsync();

    ITask<NetworkClusterNode> getNodeAsync(@NotNull String uniqueId);

    ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync();

    ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(@NotNull String uniqueId);

    NetworkClusterNode[] getNodes();

    @Nullable
    NetworkClusterNode getNode(@NotNull String uniqueId);

    NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots();

    @Nullable
    NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(@NotNull String uniqueId);

}
