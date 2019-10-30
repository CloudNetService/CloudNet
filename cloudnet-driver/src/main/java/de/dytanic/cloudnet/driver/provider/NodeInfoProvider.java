package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.command.CommandInfo;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

import java.util.Collection;

public interface NodeInfoProvider {

    Collection<CommandInfo> getConsoleCommands();

    CommandInfo getConsoleCommand(String commandLine);

    String[] sendCommandLine(String commandLine);

    String[] sendCommandLine(String nodeUniqueId, String commandLine);

    ITask<Collection<CommandInfo>> getConsoleCommandsAsync();

    ITask<CommandInfo> getConsoleCommandAsync(String commandLine);

    ITask<String[]> sendCommandLineAsync(String commandLine);

    ITask<String[]> sendCommandLineAsync(String nodeUniqueId, String commandLine);

    ITask<NetworkClusterNode[]> getNodesAsync();

    ITask<NetworkClusterNode> getNodeAsync(String uniqueId);

    ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync();

    ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(String uniqueId);

    NetworkClusterNode[] getNodes();

    NetworkClusterNode getNode(String uniqueId);

    NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots();

    NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(String uniqueId);

}
