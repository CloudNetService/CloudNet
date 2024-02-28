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
 * A runtime exception being thrown when the required no args constructor for databufable serialization is missing.
 *
 * @since 4.0
 */
public class MissingNoArgsConstructorException extends IllegalStateException {

  /**
   * Constructs a new missing no args constructor exception instance.
   *
   * @param clazz the class in which the constructor is missing.
   * @throws NullPointerException if the given class is null.
   */
  public MissingNoArgsConstructorException(@NonNull Class<?> clazz) {
    super(String.format("Missing no args constructor for class %s", clazz.getCanonicalName()));
  }
}
