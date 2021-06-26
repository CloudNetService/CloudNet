/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.driver.api;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableFunction;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientDriverAPI;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedQueryResponse;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import java.util.function.Consumer;

public interface DriverAPIUser {

  INetworkChannel getNetworkChannel();

  default ITask<ChunkedQueryResponse> executeChunkedDriverAPIMethod(DriverAPIRequestType requestType,
    Consumer<ProtocolBuffer> modifier) {
    return this.getNetworkChannel().sendChunkedPacketQuery(new PacketClientDriverAPI(requestType, modifier));
  }

  default <T> ITask<T> executeDriverAPIMethod(DriverAPIRequestType requestType, Consumer<ProtocolBuffer> modifier,
    ThrowableFunction<IPacket, T, Throwable> responseMapper) {
    return this.getNetworkChannel().sendQueryAsync(new PacketClientDriverAPI(requestType, modifier))
      .mapThrowable(responseMapper);
  }

  default <T> ITask<T> executeDriverAPIMethod(DriverAPIRequestType requestType,
    ThrowableFunction<IPacket, T, Throwable> responseMapper) {
    return this.executeDriverAPIMethod(requestType, null, responseMapper);
  }

  default ITask<Void> executeVoidDriverAPIMethod(DriverAPIRequestType requestType, Consumer<ProtocolBuffer> modifier) {
    return this.executeDriverAPIMethod(requestType, modifier, null);
  }

  default ITask<Void> executeVoidDriverAPIMethod(DriverAPIRequestType requestType, Consumer<ProtocolBuffer> modifier,
    Consumer<IPacket> responseHandler) {
    return this.executeDriverAPIMethod(requestType, modifier, packet -> {
      responseHandler.accept(packet);
      return null;
    });
  }

}
