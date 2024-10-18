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

package eu.cloudnetservice.driver.network.rpc.handler;

import eu.cloudnetservice.driver.network.rpc.RPCProvider;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * A handler for any rpc invocation happening on the network. Every handler is bound to a specific class and optionally
 * its instance it is handling. If the instance is not given, the handler is a placeholder which is only working with
 * the result of the previous method calls in chained rpc calls.
 *
 * @since 4.0
 */
public interface RPCHandler extends RPCProvider {

  /**
   * Handles the given rpc context using the information of this handler. A handler must respect all settings set in the
   * context. The handler decides whether it can handle the request based on the information provided through the
   * context. If the instance use is set to strict in the context (or this handler has no instance set since
   * construction) and no instance is provided through the context, the handler should return a successful result
   * containing either null or the default primitive value based on the return value of the target method (if normalize
   * primitives is set to true in the context). If an exception happens during the post of the rpc to the underlying
   * method the result should be a failure containing as its result the thrown exception.
   *
   * @param context the context of the handler invocation.
   * @return the result of the handler execution.
   * @throws NullPointerException if the given context is null.
   */
  @NonNull
  CompletableFuture<RPCInvocationResult> handle(@NonNull RPCInvocationContext context);

  /**
   * A builder for an RPC handler.
   *
   * @param <T> the type that the handler is handling.
   * @since 4.0
   */
  interface Builder<T> extends RPCProvider.Builder<Builder<T>> {

    /**
     * Sets the target instance on which all incoming RPC calls should be executed. If the given instance is null, the
     * invocation context must provide the instance to call the method on. If neither is present an exception is thrown
     * during execution.
     *
     * @param instance the instance on which the target method should be called.
     * @return this builder, for chaining.
     */
    @NonNull
    @Contract("_ -> this")
    Builder<T> targetInstance(@Nullable T instance);

    /**
     * Builds a new RPC handler based on the options provided to this builder.
     *
     * @return a new RPC handler for the given target class.
     */
    @NonNull
    RPCHandler build();
  }
}
