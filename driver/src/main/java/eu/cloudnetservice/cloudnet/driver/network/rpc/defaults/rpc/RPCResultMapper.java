/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.rpc;

import com.google.common.util.concurrent.UncheckedTimeoutException;
import eu.cloudnetservice.cloudnet.driver.network.protocol.Packet;
import eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.handler.util.ExceptionalResultUtil;
import eu.cloudnetservice.cloudnet.driver.network.rpc.object.ObjectMapper;
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
    // check if the query timed out before trying to read from the buffer
    if (response.readable()) {
      // the remote execution responded - check if the execution was successful or resulted in an exception
      var content = response.content();
      if (content.readBoolean()) {
        // the execution did not throw an exception
        return this.objectMapper.readObject(content, this.expectedResultType);
      } else {
        // rethrow the execution exception
        ExceptionalResultUtil.rethrowException(content);
        return null; // ok fine, but this will never happen - no one was seen again after entering the rethrowException method
      }
    } else {
      // the query timed out - just cover that case in a nice exception wrapper :(
      throw new UncheckedTimeoutException("Query future was completed before rpc was able to respond");
    }
  }
}
