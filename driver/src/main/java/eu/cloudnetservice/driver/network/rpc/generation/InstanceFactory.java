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

package eu.cloudnetservice.driver.network.rpc.generation;

import lombok.NonNull;

/**
 * Represents the factory which can be used to obtain new instances of generated rpc classes.
 *
 * @param <T> the type of the generated class.
 * @since 4.0
 */
@FunctionalInterface
public interface InstanceFactory<T> {

  /**
   * Constructs a new instance of the underlying class, using the given arguments for the constructor invocation of the
   * class.
   *
   * @param args the arguments for the constructor.
   * @return a new instance of the underlying class.
   * @throws NullPointerException if the given arguments are null.
   */
  @NonNull T newInstance(@NonNull Object... args);
}
