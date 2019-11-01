package de.dytanic.cloudnet.driver.network;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.internal.InternalSyncPacketChannel;

import java.util.function.Function;

public class PacketQueryProvider {

    private INetworkClient defaultNetworkClient;

    public PacketQueryProvider(INetworkClient defaultNetworkClient) {
        this.defaultNetworkClient = defaultNetworkClient;
    }

    public <R> ITask<R> sendCallablePacket(INetworkChannel networkChannel, String channel, String id, JsonDocument data, Function<JsonDocument, R> function) {
        Validate.checkNotNull(networkChannel);
        Validate.checkNotNull(channel);
        Validate.checkNotNull(id);
        Validate.checkNotNull(data);
        Validate.checkNotNull(function);

        return this.sendCallablePacket(networkChannel, channel, data.append(PacketConstants.SYNC_PACKET_ID_PROPERTY, id), null, jsonDocumentPair -> function.apply(jsonDocumentPair.getFirst()));
    }

    public <R> ITask<R> sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(JsonDocument header, byte[] body, Function<Pair<JsonDocument, byte[]>, R> function) {
        return this.sendCallablePacketWithAsDriverSyncAPI(this.defaultNetworkClient.getChannels().iterator().next(), header, body, function);
    }

    public <R> ITask<R> sendCallablePacketWithAsDriverSyncAPI(INetworkChannel channel, JsonDocument header, byte[] body, Function<Pair<JsonDocument, byte[]>, R> function) {
        return this.sendCallablePacket(channel, "cloudnet_driver_sync_api", header, body, function);
    }

    public <R> ITask<R> sendCallablePacket(INetworkChannel networkChannel, String channel, JsonDocument header, byte[] body, Function<Pair<JsonDocument, byte[]>, R> function) {
        return sendCallablePacket0(networkChannel, channel, header, body, function);
    }

    private <R> ITask<R> sendCallablePacket0(INetworkChannel networkChannel, String channel, JsonDocument header, byte[] body, Function<Pair<JsonDocument, byte[]>, R> function) {
        header.append(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY, channel);

        Value<R> value = new Value<>();

        ITask<R> listenableTask = new ListenableTask<>(value::getValue);

        InternalSyncPacketChannel.sendCallablePacket(networkChannel, header, body, new ITaskListener<Pair<JsonDocument, byte[]>>() {

            @Override
            public void onComplete(ITask<Pair<JsonDocument, byte[]>> task, Pair<JsonDocument, byte[]> result) {
                value.setValue(function.apply(result));
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
