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
import org.jetbrains.annotations.Nullable;

/**
 * Represents the factory which can be used to obtain new instances of generated chained rpc classes.
 *
 * @param <T> the type of the generated class.
 * @since 4.0
 */
@FunctionalInterface
public interface ChainInstanceFactory<T> {

  /**
   * Constructs a new instance of the underlying class, using the given arguments both for the base rpc creation and the
   * constructor invocation of the class.
   *
   * @param args the arguments for the base rpc and constructor.
   * @return a new instance of the underlying class.
   * @throws NullPointerException if the given arguments are null.
   */
  default @NonNull T newInstance(@NonNull Object... args) {
    return this.newInstance(args, args);
  }

  /**
   * Constructs a new instance of the underlying class, using the given arguments only to create the base rpc for the
   * class and supplying no parameters to the constructor.
   *
   * @param rpcArgs the arguments for the rpc creation.
   * @return a new instance of the underlying class.
   * @throws NullPointerException if the given rpc args are null.
   */
  default @NonNull T newRPCOnlyInstance(@NonNull Object... rpcArgs) {
    return this.newInstance(null, rpcArgs);
  }

  /**
   * Constructs a new instance of the underlying class.
   *
   * @param constructorArgs the arguments to supply to the constructor, null for no arguments.
   * @param rpcArgs         the arguments to use for the base rpc creation.
   * @return a new instance of the underlying class.
   * @throws NullPointerException if the given rpc args are null.
   */
  @NonNull T newInstance(@Nullable Object[] constructorArgs, @NonNull Object[] rpcArgs);
}
