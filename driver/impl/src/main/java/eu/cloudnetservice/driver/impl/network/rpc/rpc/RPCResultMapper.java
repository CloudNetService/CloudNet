/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.impl.network.rpc.rpc;

import eu.cloudnetservice.driver.impl.network.rpc.handler.util.RPCExceptionUtil;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.rpc.exception.RPCExecutionException;
import eu.cloudnetservice.driver.network.rpc.handler.RPCInvocationResult;
import java.lang.reflect.Type;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * The shared mapping function between the rpc chain and default rpc handling the result of a remote code execution and
 * mapping the result in a convenient way.
 *
 * @param <T> the generic expected result type of the rpc.
 * @since 4.0
 */
record RPCResultMapper<T>(
  @NonNull Type expectedResultType,
  @NonNull ObjectMapper objectMapper
) implements Function<Packet, T> {

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnknownNullability T apply(@UnknownNullability Packet response) {
    var responseData = response.content();
    var status = responseData.readByte();
    return switch (status) {
      case RPCInvocationResult.STATUS_OK -> this.objectMapper.readObject(responseData, this.expectedResultType);
      case RPCInvocationResult.STATUS_ERROR -> {
        RPCExceptionUtil.rethrowHandlingException(responseData);
        yield null; // never reached, but must be there for the compiler to be happy
      }
      case RPCInvocationResult.STATUS_BAD_REQUEST -> {
        var detailMessage = responseData.readString();
        var exceptionMessage = String.format("RPC couldn't be processed due to bad input data: %s", detailMessage);
        throw new RPCExecutionException(exceptionMessage);
      }
      case RPCInvocationResult.STATUS_SERVER_ERROR -> {
        var detailMessage = responseData.readString();
        var exceptionMessage = String.format("RPC couldn't be processed due to a server error: %s", detailMessage);
        throw new RPCExecutionException(exceptionMessage);
      }
      default -> {
        var exceptionMessage = String.format("Server responded with unknown status code: %d", status);
        throw new RPCExecutionException(exceptionMessage);
      }
    };
  }
}
