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

import eu.cloudnetservice.driver.network.rpc.introspec.RPCMethodMetadata;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The result of an RPC method invocation, either successful or failed.
 *
 * @since 4.0
 */
public sealed interface RPCInvocationResult {

  /**
   * The RPC was processed successfully on the remote side and the response contains the method invocation result.
   */
  byte STATUS_OK = 0;
  /**
   * An error was thrown during the method handling, part of the stacktrace is included in the response.
   */
  byte STATUS_ERROR = 1;
  /**
   * Indicates that the client send a bad request to the RPC handler.
   */
  byte STATUS_BAD_REQUEST = 2;
  /**
   * Indicates that there was a server error which made it impossible to handle the request.
   */
  byte STATUS_SERVER_ERROR = 3;

  /**
   * Get if the method invocation was successful or not.
   *
   * @return true if the method invocation was successful, false otherwise.
   */
  boolean wasSuccessful();

  /**
   * Get the handler that did handle the RPC request.
   *
   * @return the handler that did handle the RPC request.
   */
  @NonNull
  RPCHandler invocationHandler();

  /**
   * Represents a successful method invocation via RPC. This wraps the value returned by the called method.
   *
   * @param invocationResult  the value returned from the method invocation.
   * @param invocationHandler the handler that handled the invocation request.
   * @param invokedMethod     the method that was invoked during handling of the RPC request.
   * @since 4.0
   */
  record Success(
    @Nullable Object invocationResult,
    @NonNull RPCHandler invocationHandler,
    @NonNull RPCMethodMetadata invokedMethod
  ) implements RPCInvocationResult {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasSuccessful() {
      return true;
    }
  }

  /**
   * Represents an invocation during which the target method threw an exception.
   *
   * @param caughtException   the exception that was caught during the method invocation.
   * @param invocationHandler the handler that handled the invocation request.
   * @param invokedMethod     the method that was invoked during handling of the RPC request.
   * @since 4.0
   */
  record Failure(
    @NonNull Throwable caughtException,
    @NonNull RPCHandler invocationHandler,
    @NonNull RPCMethodMetadata invokedMethod
  ) implements RPCInvocationResult {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasSuccessful() {
      return false;
    }
  }

  /**
   * Represents an invocation result for an invocation that cannot be processed due to bad data sent by the client.
   *
   * @param detailMessage     a short detail description why the invocation wasn't possible.
   * @param invocationHandler the handler that handled the invocation request.
   * @since 4.0
   */
  record BadRequest(
    @NonNull String detailMessage,
    @NonNull RPCHandler invocationHandler
  ) implements RPCInvocationResult {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasSuccessful() {
      return false;
    }
  }

  /**
   * Represents an invocation result for an invocation that failed because there was some mismatch on the server side
   * that caused the request to be unprocessable.
   *
   * @param detailMessage     a short detail description why the invocation wasn't possible.
   * @param invocationHandler the handler that handled the invocation request.
   * @since 4.0
   */
  record ServerError(
    @NonNull String detailMessage,
    @NonNull RPCHandler invocationHandler
  ) implements RPCInvocationResult {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasSuccessful() {
      return false;
    }
  }
}
