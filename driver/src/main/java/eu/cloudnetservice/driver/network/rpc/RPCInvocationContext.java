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

package eu.cloudnetservice.driver.network.rpc;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.context.DefaultRPCInvocationContextBuilder;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The invocation context holds all information needed for a handler to process a method a call properly.
 *
 * @since 4.0
 */
public interface RPCInvocationContext {

  /**
   * Get a new builder instance for a rpc invocation context.
   *
   * @return a new invocation context builder.
   */
  static @NonNull Builder builder() {
    return new DefaultRPCInvocationContextBuilder();
  }

  /**
   * Get the amount of arguments the target method must have.
   *
   * @return the amount of arguments of the target method.
   */
  int argumentCount();

  /**
   * Get if the rpc invocation expects a result sent back to the sender.
   *
   * @return true if the rpc call expects a result, false otherwise.
   */
  boolean expectsMethodResult();

  /**
   * Get if primitive return values should be normalized when unprocessable due to for example null return values during
   * a downstream method invocation.
   *
   * @return true if primitives should be normalized, false otherwise.
   */
  boolean normalizePrimitives();

  /**
   * Get if the instance supplied to this context must be used or if the rpc handler is free to use the instance to
   * which its bound.
   *
   * @return true if the instance usage during processing should be strict, false otherwise.
   */
  boolean strictInstanceUsage();

  /**
   * Get the name of the method which should be invoked during the current rpc processing.
   *
   * @return the name of the method which should be invoked.
   */
  @NonNull String methodName();

  /**
   * Get the network channel from which the invocation request came.
   *
   * @return the network channel from which the invocation request came.
   */
  @NonNull NetworkChannel channel();

  /**
   * Get the data buffer slice which should contain the argument information for the current method invocation. The
   * buffer might however also contain information for the next rpc calls in the chain (if any).
   *
   * @return the data buffer which should contain the argument information for the current method invocation.
   */
  @NonNull DataBuf argumentInformation();

  /**
   * Get the instance (if any) which should be considered first for a method invocation. This is used for calling
   * methods based on the result of the previous rpc in a chained rpc.
   *
   * @return the instance to use for calling the requested method (if any).
   */
  @Nullable Object workingInstance();

  /**
   * Represents the builder for a rpc invocation context.
   *
   * @since 4.0
   */
  interface Builder {

    /**
     * Sets the amount of arguments the target method to invoke must have.
     *
     * @param argumentCount the amount of arguments the target method must have.
     * @return the same builder instance as used to call the method, for chaining.
     */
    @NonNull Builder argumentCount(int argumentCount);

    /**
     * Sets if the sender of the rpc expects a method result.
     *
     * @param expectsResult if a method result is expected.
     * @return the same builder instance as used to call the method, for chaining.
     */
    @NonNull Builder expectsMethodResult(boolean expectsResult);

    /**
     * Sets if primitives should get normalized, meaning that if a null value is returned in a call chain but the method
     * returns a primitive type, the response will be the default value of that primitive rather than null.
     *
     * @param normalizePrimitives if nulls should get normalized if a primitive is expected.
     * @return the same builder instance as used to call the method, for chaining.
     */
    @NonNull Builder normalizePrimitives(boolean normalizePrimitives);

    /**
     * Sets if the handler is only allowed to use the supplied instance through the invocation context rather than the
     * instance it is bound to (if any).
     *
     * @param strictInstanceUsage if the instance usage for method invocation should be strict.
     * @return the same builder instance as used to call the method, for chaining.
     */
    @NonNull Builder strictInstanceUsage(boolean strictInstanceUsage);

    /**
     * Sets the name of the method which should get invoked during the requested rpc processing.
     *
     * @param methodName the name of the method to call.
     * @return the same builder instance as used to call the method, for chaining.
     * @throws NullPointerException if the given method name is null.
     */
    @NonNull Builder methodName(@NonNull String methodName);

    /**
     * Sets the channel from which the request to invoke the method came.
     *
     * @param channel the channel to which the request was sent.
     * @return the same builder instance as used to call the method, for chaining.
     * @throws NullPointerException if the given network channel is null.
     */
    @NonNull Builder channel(@NonNull NetworkChannel channel);

    /**
     * Sets the buffer containing the argument information for the method invocation. The buffer is allowed to hold more
     * information than that <strong>AFTER</strong> the method arguments.
     *
     * @param information the buffer holding the arguments for the method call.
     * @return the same builder instance as used to call the method, for chaining.
     * @throws NullPointerException if the given buffer is null.
     */
    @NonNull Builder argumentInformation(@NonNull DataBuf information);

    /**
     * Sets the instance which should get used for method invocation, might be null if no specific instance should be
     * used. If strict instance usage is active and no instance was supplied through the method, the result of the rpc
     * invocation will always be null (or a normalized primitive value if active).
     *
     * @param instance the instance to use for method calls.
     * @return the same builder instance as used to call the method, for chaining.
     */
    @NonNull Builder workingInstance(@Nullable Object instance);

    /**
     * Builds a new invocation context based on the arguments supplied to this builder.
     *
     * @return a new invocation context from this builder.
     * @throws NullPointerException if either the channel, method name or argument buffer was not supplied.
     */
    @NonNull RPCInvocationContext build();
  }
}
