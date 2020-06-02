package de.dytanic.cloudnet.wrapper;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.wrapper.network.packet.PacketClientDriverAPI;

import java.util.function.Consumer;
import java.util.function.Function;

public interface DriverAPIUser {

    INetworkClient getNetworkClient();

    default <T> ITask<T> executeDriverAPIMethod(DriverAPIRequestType requestType, Consumer<ProtocolBuffer> bodyModifier, Function<IPacket, T> responseMapper) {
        CompletableTask<T> task = new CompletableTask<>();

        this.getNetworkClient().getFirstChannel().sendQueryAsync(new PacketClientDriverAPI(requestType, bodyModifier))
                .onComplete(packet -> task.complete(responseMapper.apply(packet)))
                .onCancelled(v -> task.cancel(true))
                .addListener(ITaskListener.FIRE_EXCEPTION_ON_FAILURE);

        return task;
    }

    default <T> ITask<T> executeDriverAPIMethod(DriverAPIRequestType requestType, Function<IPacket, T> responseMapper) {
        return this.executeDriverAPIMethod(requestType, null, responseMapper);
    }

    default <T> ITask<T> executeVoidDriverAPIMethod(DriverAPIRequestType requestType, Consumer<ProtocolBuffer> bodyModifier) {
        return this.executeDriverAPIMethod(requestType, bodyModifier, null);
    }

    default <T> ITask<T> executeVoidDriverAPIMethod(DriverAPIRequestType requestType, Consumer<ProtocolBuffer> bodyModifier, Consumer<IPacket> responseHandler) {
        return this.executeDriverAPIMethod(requestType, bodyModifier, packet -> {
            responseHandler.accept(packet);
            return null;
        });
    }

}
