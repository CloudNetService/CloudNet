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

package eu.cloudnetservice.node.command.exception;

import lombok.NonNull;

/**
 * This exception is used when argument parsers need to hard fail the argument parsing because there is a syntax error.
 * The message of the exception is sent to the user therefore it should be translated and formatted correctly.
 * <p>
 * Note: The {@link ArgumentNotAvailableException#fillInStackTrace()} method is empty, therefore the creation of this
 * exception is not heavy, and it can be used frequently.
 *
 * @since 4.0
 */
public class ArgumentNotAvailableException extends RuntimeException {

  /**
   * Constructs a new exception for argument parse failing.
   *
   * @param message the message to send to the user.
   * @throws NullPointerException if message is null.
   */
  public ArgumentNotAvailableException(@NonNull String message) {
    super(message);
  }

  /**
   * Returns the own instance of this exception without filling the stacktrace, as the stacktrace is not needed for this
   * exception.
   *
   * @return this instance for chaining.
   */
  @Override
  public @NonNull Throwable fillInStackTrace() {
    return this;
  }
}
