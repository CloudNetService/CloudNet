package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.internal.InternalSyncPacketChannel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class PacketQueryProvider {

    private INetworkClient defaultNetworkClient;

    public PacketQueryProvider(@NotNull INetworkClient defaultNetworkClient) {
        this.defaultNetworkClient = defaultNetworkClient;
    }

    @NotNull
    public <R> ITask<R> sendCallablePacket(@NotNull INetworkChannel networkChannel, @NotNull String channel, @NotNull String id, @NotNull JsonDocument data, @NotNull Function<JsonDocument, R> function) {
        return this.sendCallablePacket(networkChannel, channel, data.append(PacketConstants.SYNC_PACKET_ID_PROPERTY, id), null, jsonDocumentPair -> function.apply(jsonDocumentPair.getFirst()));
    }

    @NotNull
    public <R> ITask<R> sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(@NotNull JsonDocument header, byte[] body, @NotNull Function<Pair<JsonDocument, byte[]>, R> function) {
        return this.sendCallablePacketWithAsDriverSyncAPI(this.defaultNetworkClient.getChannels().iterator().next(), header, body, function);
    }

    @NotNull
    public <R> ITask<R> sendCallablePacketWithAsDriverSyncAPI(@NotNull INetworkChannel channel, @NotNull JsonDocument header, byte[] body, @NotNull Function<Pair<JsonDocument, byte[]>, R> function) {
        return this.sendCallablePacket(channel, "cloudnet_driver_sync_api", header, body, function);
    }

    @NotNull
    public <R> ITask<R> sendCallablePacket(@NotNull INetworkChannel networkChannel, String channel, @NotNull JsonDocument header, byte[] body, @NotNull Function<Pair<JsonDocument, byte[]>, R> function) {
        return sendCallablePacket0(networkChannel, channel, header, body, function);
    }

    @NotNull
    private <R> ITask<R> sendCallablePacket0(@NotNull INetworkChannel networkChannel, String channel, @NotNull JsonDocument header, byte[] body, @NotNull Function<Pair<JsonDocument, byte[]>, R> function) {
        header.append(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY, channel);

        AtomicReference<R> reference = new AtomicReference<>();

        ITask<R> listenableTask = new ListenableTask<>(reference::get);

        InternalSyncPacketChannel.sendCallablePacket(networkChannel, header, body, new ITaskListener<Pair<JsonDocument, byte[]>>() {

            @Override
            public void onComplete(ITask<Pair<JsonDocument, byte[]>> task, Pair<JsonDocument, byte[]> result) {
                reference.set(function.apply(result));
                try {
                    listenableTask.call();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            @Override
            public void onFailure(ITask<Pair<JsonDocument, byte[]>> task, Throwable th) {
                th.printStackTrace();
            }
        });

        return listenableTask;
    }


}
