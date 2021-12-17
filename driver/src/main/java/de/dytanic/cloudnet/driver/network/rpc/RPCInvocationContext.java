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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface RPCInvocationContext {

  static @NonNull Builder builder() {
    return new DefaultRPCInvocationContextBuilder();
  }

  int argumentCount();

  boolean expectsMethodResult();

  boolean normalizePrimitives();

  boolean strictInstanceUsage();

  @NonNull String methodName();

  @NonNull INetworkChannel channel();

  @NonNull DataBuf argumentInformation();

  @NonNull Optional<Object> workingInstance();

  interface Builder {

    @NonNull Builder argumentCount(int argumentCount);

    @NonNull Builder expectsMethodResult(boolean expectsResult);

    @NonNull Builder normalizePrimitives(boolean normalizePrimitives);

    @NonNull Builder strictInstanceUsage(boolean strictInstanceUsage);

    @NonNull Builder methodName(@NonNull String methodName);

    @NonNull Builder channel(@NonNull INetworkChannel channel);

    @NonNull Builder argumentInformation(@NonNull DataBuf information);

    @NonNull Builder workingInstance(@Nullable Object instance);

    @NonNull RPCInvocationContext build();
  }
}
