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

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * The invocation context holds all information needed for a handler to process a method a call properly.
 *
 * @since 4.0
 */
public interface RPCInvocationContext {

  /**
   * Get the name of the method which should be invoked during the current rpc processing.
   *
   * @return the name of the method which should be invoked.
   */
  @NonNull
  String methodName();

  /**
   * Get the descriptor of the method that should be executed.
   *
   * @return the descriptor of the method that should be executed.
   */
  @NonNull
  String methodDescriptor();

  /**
   * Get the data buffer slice which should contain the argument information for the current method invocation. The
   * buffer can contain more, unrelated data.
   *
   * @return the data buffer which should contain the argument information for the current method invocation.
   */
  @NonNull
  DataBuf argumentInformation();

  /**
   * Get the instance (if any) which should be considered first for a method invocation. This is used for calling
   * methods based on the result of the previous rpc in a chained rpc.
   *
   * @return the instance to use for calling the requested method (if any).
   */
  @Nullable
  Object workingInstance();

  /**
   * Represents the builder for a rpc invocation context.
   *
   * @since 4.0
   */
  interface Builder {

    /**
     * Sets the name of the method that should be called.
     *
     * @param methodName the name of the method to call.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given method name is null.
     */
    @NonNull
    @Contract("_ -> this")
    Builder methodName(@NonNull String methodName);

    /**
     * Sets the method descriptor of the method that should be called.
     *
     * @param methodDescriptor the descriptor of the method to call.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given method descriptor is null.
     */
    @NonNull
    @Contract("_ -> this")
    Builder methodDescriptor(@NonNull String methodDescriptor);

    /**
     * Sets the buffer containing the argument information for the method invocation. The buffer can be suffixed with
     * more information that the arguments.
     *
     * @param information the buffer holding the arguments for the method call.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given buffer is null.
     */
    @NonNull
    @Contract("_ -> this")
    Builder argumentInformation(@NonNull DataBuf information);

    /**
     * Sets the instance which should get used for method invocation, might be null if no specific instance should be
     * used. If given, the handler will consider this instance first and fall back to the bound instance if not given.
     *
     * @param instance the instance to use for method calls.
     * @return this builder, for chaining.
     */
    @NonNull
    @Contract("_ -> this")
    Builder workingInstance(@Nullable Object instance);

    /**
     * Builds a new invocation context based on the arguments supplied to this builder.
     *
     * @return a new invocation context from this builder.
     * @throws NullPointerException if either the channel, method name or argument buffer was not supplied.
     */
    @NonNull
    @Contract("-> new")
    RPCInvocationContext build();
  }
}
