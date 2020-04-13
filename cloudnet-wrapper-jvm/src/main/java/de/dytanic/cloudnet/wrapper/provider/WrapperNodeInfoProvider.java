package de.dytanic.cloudnet.wrapper.provider;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.command.CommandInfo;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.provider.NodeInfoProvider;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WrapperNodeInfoProvider implements NodeInfoProvider {

    private final Wrapper wrapper;

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

    @Nullable
    @Override
    public CommandInfo getConsoleCommand(@NotNull String commandLine) {
        try {
            return this.getConsoleCommandAsync(commandLine).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<String> getConsoleTabCompleteResults(@NotNull String commandLine) {
        try {
            return this.getConsoleTabCompleteResultsAsync(commandLine).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public String[] sendCommandLine(@NotNull String commandLine) {
        Preconditions.checkNotNull(commandLine);

        try {
            return this.sendCommandLineAsync(commandLine).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public String[] sendCommandLine(@NotNull String nodeUniqueId, @NotNull String commandLine) {
        Preconditions.checkNotNull(nodeUniqueId, commandLine);

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

    @Nullable
    @Override
    public NetworkClusterNode getNode(@NotNull String uniqueId) {
        Preconditions.checkNotNull(uniqueId);

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

    @Nullable
    @Override
    public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(@NotNull String uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        try {
            return this.getNodeInfoSnapshotAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    @NotNull
    public ITask<Collection<CommandInfo>> getConsoleCommandsAsync() {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "console_commands"),
                new byte[0],
                pair -> pair.getFirst().get("commandInfos", new TypeToken<Collection<CommandInfo>>() {
                }.getType())
        );
    }

    @Override
    @NotNull
    public ITask<CommandInfo> getConsoleCommandAsync(@NotNull String commandLine) {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "console_commands")
                        .append("commandLine", commandLine),
                new byte[0],
                pair -> pair.getFirst().get("commandInfo", CommandInfo.class)
        );
    }

    @Override
    @NotNull
    public ITask<Collection<String>> getConsoleTabCompleteResultsAsync(@NotNull String commandLine) {
        Preconditions.checkNotNull(commandLine);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "tab_complete").append("commandLine", commandLine), null,
                documentPair -> documentPair.getFirst().get("responses", new TypeToken<Collection<String>>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<String[]> sendCommandLineAsync(@NotNull String commandLine) {
        Preconditions.checkNotNull(commandLine);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandLine").append("commandLine", commandLine), null,
                documentPair -> documentPair.getFirst().get("responseMessages", new TypeToken<String[]>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<String[]> sendCommandLineAsync(@NotNull String nodeUniqueId, @NotNull String commandLine) {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandLine_on_node")
                        .append("nodeUniqueId", nodeUniqueId)
                        .append("commandLine", commandLine), null,
                documentPair -> documentPair.getFirst().get("responseMessages", new TypeToken<String[]>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<NetworkClusterNode[]> getNodesAsync() {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_nodes"), null,
                documentPair -> documentPair.getFirst().get("nodes", new TypeToken<NetworkClusterNode[]>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<NetworkClusterNode> getNodeAsync(@NotNull String uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_node_by_uniqueId").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("clusterNode", new TypeToken<NetworkClusterNode>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync() {
        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_node_info_snapshots"), null,
                documentPair -> documentPair.getFirst().get("nodeInfoSnapshots", new TypeToken<NetworkClusterNodeInfoSnapshot[]>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(@NotNull String uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_node_info_snapshot_by_uniqueId").append("uniqueId", uniqueId), null,
                documentPair -> documentPair.getFirst().get("clusterNodeInfoSnapshot", new TypeToken<NetworkClusterNodeInfoSnapshot>() {
                }.getType()));
    }

}
