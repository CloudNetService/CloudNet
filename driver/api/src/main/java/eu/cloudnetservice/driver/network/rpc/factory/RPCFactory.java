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

package eu.cloudnetservice.driver.network.rpc.factory;

import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandler;
import lombok.NonNull;

/**
 * A factory for the root RPC types.
 *
 * @since 4.0
 */
public interface RPCFactory {

  /**
   * Constructs a new builder for an RPC sender which can provide RPCs for the given target class and all subclasses in
   * the tree.
   *
   * @param target the target class for which the rpc sender should be constructed.
   * @return a builder for an RPC sender that targets the given target class.
   * @throws NullPointerException     if the given target class is null.
   * @throws IllegalArgumentException if some precondition, to ensure functionality with rpc, fails.
   * @throws IllegalStateException    if some precondition, to ensure functionality with rpc, fails.
   */
  @NonNull
  RPCSender.Builder newRPCSenderBuilder(@NonNull Class<?> target);

  /**
   * Constructs a new rpc handler builder which can handle RPC requests for the given target class.
   *
   * @param target the target class that should be handled by the RPC handler.
   * @param <T>    the target type.
   * @return a new builder for an RPC handler which can handle RPC requests for the given target class.
   * @throws NullPointerException     if the given target class is null.
   * @throws IllegalArgumentException if some precondition, to ensure functionality with rpc, fails.
   * @throws IllegalStateException    if some precondition, to ensure functionality with rpc, fails.
   */
  @NonNull
  <T> RPCHandler.Builder<T> newRPCHandlerBuilder(@NonNull Class<T> target);

  /**
   * Constructs a builder for an RPC-based api implementation for all non-static methods in the given base class.
   *
   * @param baseClass the base class which should be implemented.
   * @param <T>       the type being implemented.
   * @return a builder for a base implementation of the given base class.
   * @throws NullPointerException     if the given base class is null.
   * @throws IllegalArgumentException if some precondition, to ensure functionality with rpc, fails.
   * @throws IllegalStateException    if some precondition, to ensure functionality with rpc, fails.
   */
  @NonNull
  <T> RPCImplementationBuilder<T> newRPCBasedImplementationBuilder(@NonNull Class<T> baseClass);
}
