/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.driver.network.rpc.defaults.MethodInformation;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * A handler for any rpc invocation happening on the network. Every handler is bound to a specific class and optionally
 * its instance it is handling. If the instance is not given, the handler is a placeholder which is only working with
 * the result of the previous method calls in chained rpc calls.
 *
 * @since 4.0
 */
public interface RPCHandler extends RPCProvider {

  /**
   * Registers this rpc handler to the given rpc handler registry.
   *
   * @param registry the registry to register to.
   * @throws NullPointerException if the given registry is null.
   */
  void registerTo(@NonNull RPCHandlerRegistry registry);

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
  @NonNull HandlingResult handle(@NonNull RPCInvocationContext context);

  /**
   * Represents the result of a method invocation with rpc.
   *
   * @since 4.0
   */
  interface HandlingResult {

    /**
     * Get if the method invocation was successful or not.
     *
     * @return true if the method invocation was successful, false otherwise.
     */
    boolean wasSuccessful();

    /**
     * Get the actual invocation result wrapped in this class. This method is only nullable if the result of the
     * invocation was successful (then the method result was null), otherwise the invocation result must be non-null and
     * a subtype of a throwable.
     *
     * @return the unwrapped invocation result.
     */
    @UnknownNullability Object invocationResult();

    /**
     * The handler which handled the invocation request and created the result wrapper based on the output of the
     * method.
     *
     * @return the handler which handled the invocation request.
     */
    @NonNull RPCHandler invocationHandler();

    /**
     * Get the information about the method which was invoked by the handler and returned the data wrapped in this
     * result.
     *
     * @return the information about the method which was invoked by the handler.
     */
    @NonNull MethodInformation targetMethodInformation();
  }
}
