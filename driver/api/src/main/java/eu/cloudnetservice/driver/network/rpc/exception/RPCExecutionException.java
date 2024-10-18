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

package eu.cloudnetservice.driver.network.rpc.exception;

import lombok.NonNull;

/**
 * An exception thrown when an RPC terminated unsuccessfully, for example because the remote method threw an exception
 * or invalid data was supplied.
 *
 * @since 4.0
 */
public final class RPCExecutionException extends RuntimeException {

  /**
   * Constructs a new RPC execution exception with the given message.
   *
   * @param message the message to use for the exception.
   * @throws NullPointerException if the given message is null.
   */
  public RPCExecutionException(@NonNull String message) {
    super(message, null);
  }

  /**
   * Constructs a new rpc execution exception instance, with a formatted human-readable message based on the supplied
   * arguments.
   *
   * @param exceptionName the name of the original thrown exception.
   * @param message       the message of the original thrown exception.
   * @throws NullPointerException if either the given exception name or message is null.
   */
  public RPCExecutionException(@NonNull String exceptionName, @NonNull String message) {
    this(String.format("%s: %s", exceptionName, message));
  }
}
