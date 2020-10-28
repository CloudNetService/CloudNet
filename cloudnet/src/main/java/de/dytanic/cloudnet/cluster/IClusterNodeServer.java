package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IClusterNodeServer extends DriverAPIUser, NodeServer {

    /**
     * @deprecated Use {@link #sendCustomChannelMessage(ChannelMessage)} instead
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval
    void sendCustomChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument data);

    void sendCustomChannelMessage(@NotNull ChannelMessage channelMessage);

    @NotNull
    ITask<ChannelMessage> sendChannelMessageQuery(@NotNull ChannelMessage channelMessage);

    @NotNull
    IClusterNodeServerProvider getProvider();

    @Override @Nullable NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot();

    /**
     * @deprecated Use {@link #getNetworkChannel()} instead
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval
    INetworkChannel getChannel();

    void setChannel(@NotNull INetworkChannel channel);

    boolean isConnected();

    void saveSendPacket(@NotNull IPacket packet);

    boolean isAcceptableConnection(@NotNull INetworkChannel channel, @NotNull String nodeId);
}