package de.dytanic.cloudnet.wrapper.provider;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.provider.NodeInfoProvider;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class WrapperNodeInfoProvider implements NodeInfoProvider, DriverAPIUser {

    private final Wrapper wrapper;

    public WrapperNodeInfoProvider(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public Collection<CommandInfo> getConsoleCommands() {
        return this.getConsoleCommandsAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Nullable
    @Override
    public CommandInfo getConsoleCommand(@NotNull String commandLine) {
        return this.getConsoleCommandAsync(commandLine).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public Collection<String> getConsoleTabCompleteResults(@NotNull String commandLine) {
        return this.getConsoleTabCompleteResultsAsync(commandLine).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public String[] sendCommandLine(@NotNull String commandLine) {
        Preconditions.checkNotNull(commandLine);
        return this.sendCommandLineAsync(commandLine).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public String[] sendCommandLine(@NotNull String nodeUniqueId, @NotNull String commandLine) {
        Preconditions.checkNotNull(nodeUniqueId, commandLine);
        return this.sendCommandLineAsync(nodeUniqueId, commandLine).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public NetworkClusterNode[] getNodes() {
        return this.getNodesAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Nullable
    @Override
    public NetworkClusterNode getNode(@NotNull String uniqueId) {
        Preconditions.checkNotNull(uniqueId);
        return this.getNodeAsync(uniqueId).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public NetworkClusterNodeInfoSnapshot[] getNodeInfoSnapshots() {
        return this.getNodeInfoSnapshotsAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Nullable
    @Override
    public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot(@NotNull String uniqueId) {
        Preconditions.checkNotNull(uniqueId);
        return this.getNodeInfoSnapshotAsync(uniqueId).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    @NotNull
    public ITask<Collection<CommandInfo>> getConsoleCommandsAsync() {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_CONSOLE_COMMANDS,
                packet -> packet.getBuffer().readObjectCollection(CommandInfo.class)
        );
    }

    @Override
    @NotNull
    public ITask<CommandInfo> getConsoleCommandAsync(@NotNull String commandLine) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_CONSOLE_COMMAND_BY_LINE,
                buffer -> buffer.writeString(commandLine),
                packet -> packet.getBuffer().readObject(CommandInfo.class)
        );
    }

    @Override
    @NotNull
    public ITask<Collection<String>> getConsoleTabCompleteResultsAsync(@NotNull String commandLine) {
        Preconditions.checkNotNull(commandLine);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.TAB_COMPLETE_CONSOLE_COMMAND,
                buffer -> buffer.writeString(commandLine),
                packet -> packet.getBuffer().readStringCollection()
        );
    }

    @Override
    @NotNull
    public ITask<String[]> sendCommandLineAsync(@NotNull String commandLine) {
        Preconditions.checkNotNull(commandLine);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.SEND_COMMAND_LINE,
                buffer -> buffer.writeString(commandLine),
                packet -> packet.getBuffer().readStringArray()
        );
    }

    @Override
    @NotNull
    public ITask<String[]> sendCommandLineAsync(@NotNull String nodeUniqueId, @NotNull String commandLine) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.SEND_COMMAND_LINE_TO_NODE,
                buffer -> buffer.writeString(nodeUniqueId).writeString(commandLine),
                packet -> packet.getBuffer().readStringArray()
        );
    }

    @Override
    @NotNull
    public ITask<NetworkClusterNode[]> getNodesAsync() {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_NODES,
                packet -> packet.getBuffer().readObjectArray(NetworkClusterNode.class)
        );
    }

    @Override
    @NotNull
    public ITask<NetworkClusterNode> getNodeAsync(@NotNull String uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_NODE_BY_UNIQUE_ID,
                buffer -> buffer.writeString(uniqueId),
                packet -> packet.getBuffer().readOptionalObject(NetworkClusterNode.class)
        );
    }

    @Override
    @NotNull
    public ITask<NetworkClusterNodeInfoSnapshot[]> getNodeInfoSnapshotsAsync() {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_NODE_INFO_SNAPSHOTS,
                packet -> packet.getBuffer().readObjectArray(NetworkClusterNodeInfoSnapshot.class)
        );
    }

    @Override
    @NotNull
    public ITask<NetworkClusterNodeInfoSnapshot> getNodeInfoSnapshotAsync(@NotNull String uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_NODE_INFO_SNAPSHOT_BY_UNIQUE_ID,
                buffer -> buffer.writeString(uniqueId),
                packet -> packet.getBuffer().readOptionalObject(NetworkClusterNodeInfoSnapshot.class)
        );
    }

    @Override
    public INetworkChannel getNetworkChannel() {
        return this.wrapper.getNetworkChannel();
    }
}
