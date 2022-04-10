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

package eu.cloudnetservice.driver.network.rpc.defaults.handler;

import eu.cloudnetservice.driver.network.rpc.RPCHandler;
import eu.cloudnetservice.driver.network.rpc.RPCHandler.HandlingResult;
import eu.cloudnetservice.driver.network.rpc.defaults.MethodInformation;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of a method invocation.
 *
 * @param wasSuccessful           if the method invocation was successful or ended by an exception.
 * @param invocationResult        the result of the method invocation.
 * @param invocationHandler       the handler which is responsible for the class the method wss invoked in.
 * @param targetMethodInformation the information of the method which was invoked.
 * @since 4.0
 */
public record DefaultHandlingResult(
  boolean wasSuccessful,
  @Nullable Object invocationResult,
  @NonNull RPCHandler invocationHandler,
  @NonNull MethodInformation targetMethodInformation
) implements HandlingResult {

  /**
   * Constructs a new successful invocation result with the information provided.
   *
   * @param methodInformation the information of the method which was invoked.
   * @param invocationHandler the handler which is responsible for the class the method wss invoked in.
   * @param result            the result of the method invocation.
   * @return the constructed method invocation result.
   * @throws NullPointerException if either the given method information or handler is null.
   */
  public static @NonNull HandlingResult success(
    @NonNull MethodInformation methodInformation,
    @NonNull RPCHandler invocationHandler,
    @Nullable Object result
  ) {
    return new DefaultHandlingResult(true, result, invocationHandler, methodInformation);
  }

  /**
   * Constructs a new failed invocation result, indicating that an exception was thrown as the result of the method
   * call.
   *
   * @param information       the information of the method which was invoked.
   * @param invocationHandler the handler which is responsible for the class the method wss invoked in.
   * @param result            the exception which was thrown during the method invocation.
   * @return the constructed method invocation result.
   * @throws NullPointerException if either the given method information, handler or exception is null.
   */
  public static @NonNull HandlingResult failure(
    @NonNull MethodInformation information,
    @NonNull RPCHandler invocationHandler,
    @NonNull Throwable result
  ) {
    return new DefaultHandlingResult(false, result, invocationHandler, information);
  }
}
