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
 * A runtime exception being thrown when the method selector for rpc cannot decide which method to use for the requested
 * remote call because either no method or multiple methods are matching the given filter options.
 *
 * @since 4.0
 */
public class CannotDecideException extends RuntimeException {

  /**
   * Constructs a new cannot decide exception wrapping the given method name into a nice readable message.
   *
   * @param methodName the name of the method which cannot be found.
   * @throws NullPointerException if the given method name is null.
   */
  public CannotDecideException(@NonNull String methodName) {
    super(String.format(
      "Cannot decide which method to call by rpc; expected exactly one method with name %s.",
      methodName));
  }
}
