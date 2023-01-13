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

package eu.cloudnetservice.driver.network.rpc.exception;

import java.util.Arrays;
import lombok.NonNull;

/**
 * A runtime exception being thrown when the constructor with all arguments of a class included for rpc (de-)
 * serialization is missing.
 *
 * @since 4.0
 */
public class MissingAllArgsConstructorException extends IllegalStateException {

  /**
   * Constructs a new missing all args constructor exception instance.
   *
   * @param clazz     the class in which the constructor is missing.
   * @param arguments the argument types which the constructor is missing.
   * @throws NullPointerException if the given class or arguments array is null.
   */
  public MissingAllArgsConstructorException(@NonNull Class<?> clazz, @NonNull Class<?>[] arguments) {
    super(String.format(
      "Missing all args constructor for class %s with args %s",
      clazz.getCanonicalName(),
      Arrays.asList(arguments)));
  }
}
