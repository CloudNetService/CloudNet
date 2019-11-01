package de.dytanic.cloudnet.wrapper.provider;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.command.CommandInfo;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.provider.NodeInfoProvider;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WrapperNodeInfoProvider implements NodeInfoProvider {

    private Wrapper wrapper;

    public WrapperNodeInfoProvider(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public Collection<CommandInfo> getConsoleCommands() {
        try {
            return this.getConsoleCommandsAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public CommandInfo getConsoleCommand(String commandLine) {
        try {
            return this.getConsoleCommandAsync(commandLine).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public String[] sendCommandLine(String commandLine) {
        Validate.checkNotNull(commandLine);

        try {
            return this.sendCommandLineAsync(commandLine).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public String[] sendCommandLine(String nodeUniqueId, String commandLine) {
        Validate.checkNotNull(nodeUniqueId, commandLine);

        try {
            return this.sendCommandLineAsync(nodeUniqueId, commandLine).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public NetworkClusterNode[] getNodes() {
        try {
            return this.getNodesAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public NetworkClusterNode getNode(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        try {
            return this.getNodeAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots() {
        try {
            return this.getNodeInfoSnapshotsAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        try {
            return this.getNodeInfoSnapshotAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public ITask<Collection<CommandInfo>> getConsoleCommandsAsync() {
        return this.wrapper.getPacketStation().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "console_commands"),
                new byte[0],
                pair -> pair.getFirst().get("commandInfos", new TypeToken<Collection<CommandInfo>>() {
                }.getType())
        );
    }

    @Override
    public ITask<CommandInfo> getConsoleCommandAsync(String commandLine) {
        return this.wrapper.getPacketStation().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "console_commands")
                        .append("commandLine", commandLine),
                new byte[0],
                pair -> pair.getFirst().get("commandInfo", CommandInfo.class)
        );
    }

    @Override
    public ITask<String[]> sendCommandLineAsync(String commandLine) {
        Validate.checkNotNull(commandLine);

        return this.wrapper.getPacketStation().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandLine").append("commandLine", commandLine), null,
                documentPair -> documentPair.getFirst().get("responseMessages", new TypeToken<String[]>() {
                }.getType()));
    }

    @Override
    public ITask<String[]> sendCommandLineAsync(String nodeUniqueId, String commandLine) {
        return this.wrapper.getPacketStation().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandLine_on_node")
                        .append("nodeUniqueId", nodeUniqueId)
                        .append("commandLine", commandLine), null,
                documentPair -> documentPair.getFirst().get("responseMessages", new TypeToken<String[]>() {
                }.getType()));
    }

    @Override
    public ITask<NetworkClusterNode[]> getNodesAsync() {
        return this.wrapper.getPacketStation().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_nodes"), null,
                documentPair -> documentPair.getFirst().get("nodes", new TypeToken<NetworkClusterNode[]>() {
                }.getType()));
    }

    @Override
    public ITask<NetworkClusterNode> getNodeAsync(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.wrapper.getPacketStation().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_node_by_uniqueId").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("clusterNode", new TypeToken<NetworkClusterNode>() {
                }.getType()));
    }

    @Override
    public ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync() {
        return this.wrapper.getPacketStation().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_node_info_snapshots"), null,
                documentPair -> documentPair.getFirst().get("nodeInfoSnapshots", new TypeToken<NetworkClusterNodeInfoSnapshot[]>() {
                }.getType()));
    }

    @Override
    public ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.wrapper.getPacketStation().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_node_info_snapshot_by_uniqueId").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("clusterNodeInfoSnapshot", new TypeToken<NetworkClusterNodeInfoSnapshot>() {
                }.getType()));
    }

}
