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

package de.dytanic.cloudnet.driver.network.rpc;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.context.DefaultRPCInvocationContextBuilder;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RPCInvocationContext {

  static @NotNull Builder builder() {
    return new DefaultRPCInvocationContextBuilder();
  }

  boolean expectsMethodResult();

  boolean normalizePrimitives();

  @NotNull String getMethodName();

  @NotNull INetworkChannel getChannel();

  @NotNull DataBuf getArgumentInformation();

  @NotNull Optional<Object> getWorkingInstance();

  interface Builder {

    @NotNull Builder expectsMethodResult(boolean expectsResult);

    @NotNull Builder normalizePrimitives(boolean normalizePrimitives);

    @NotNull Builder methodName(@NotNull String methodName);

    @NotNull Builder channel(@NotNull INetworkChannel channel);

    @NotNull Builder argumentInformation(@NotNull DataBuf information);

    @NotNull Builder workingInstance(@Nullable Object instance);

    @NotNull RPCInvocationContext build();
  }
}
