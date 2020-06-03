package de.dytanic.cloudnet.driver.api;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientDriverAPI;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

import java.util.function.Consumer;
import java.util.function.Function;

public interface DriverAPIUser {

    INetworkChannel getNetworkChannel();

    default <T> ITask<T> executeDriverAPIMethod(DriverAPIRequestType requestType, Consumer<ProtocolBuffer> modifier, Function<IPacket, T> responseMapper) {
        return this.getNetworkChannel().sendQueryAsync(new PacketClientDriverAPI(requestType, modifier)).map(responseMapper);
    }

    default <T> ITask<T> executeDriverAPIMethod(DriverAPIRequestType requestType, Function<IPacket, T> responseMapper) {
        return this.executeDriverAPIMethod(requestType, null, responseMapper);
    }

    default ITask<Void> executeVoidDriverAPIMethod(DriverAPIRequestType requestType, Consumer<ProtocolBuffer> modifier) {
        return this.executeDriverAPIMethod(requestType, modifier, null);
    }

    default ITask<Void> executeVoidDriverAPIMethod(DriverAPIRequestType requestType, Consumer<ProtocolBuffer> modifier, Consumer<IPacket> responseHandler) {
        return this.executeDriverAPIMethod(requestType, modifier, packet -> {
            responseHandler.accept(packet);
            return null;
        });
    }

}
