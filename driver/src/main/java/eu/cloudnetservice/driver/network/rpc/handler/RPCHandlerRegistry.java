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

import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The registry for all rpc handler, distributed to a packet listener for the rpc channel to handle incoming rpc
 * requests. All class names are in the canonical format.
 *
 * @since 4.0
 */
public interface RPCHandlerRegistry {

  /**
   * Get all handlers which are registered to this registry. The keys of the map is the target class of the handler
   * registered as its value.
   *
   * @return all handlers which are registered to this registry.
   */
  @NonNull
  @UnmodifiableView
  Map<String, RPCHandler> registeredHandlers();

  /**
   * Checks if this registry has a handler for the given target class.
   *
   * @param targetClass the class to check for.
   * @return true if this registry has a handler for the given class, false otherwise.
   * @throws NullPointerException if the given target class is null.
   */
  boolean hasHandler(@NonNull Class<?> targetClass);

  /**
   * Checks if this registry has a handler for the given target class name.
   *
   * @param targetClassName the name of the class to check for.
   * @return true if this registry has a handler for the given target class, false otherwise.
   * @throws NullPointerException if the given class name is null.
   */
  boolean hasHandler(@NonNull String targetClassName);

  /**
   * Get the handler which is registered for the given target class or null if no handler for the class is registered.
   *
   * @param targetClass the class to get the rpc handler for.
   * @return the registered rpc handler for the class or null if no handler is registered.
   * @throws NullPointerException if the given class is null.
   */
  @Nullable RPCHandler handler(@NonNull Class<?> targetClass);

  /**
   * Get the handler which is registered for the given class by its name or null if no handler for the class is
   * registered.
   *
   * @param targetClassName the name of the class to get the handler for.
   * @return the registered rpc handler for the class or null if no handler is registered.
   * @throws NullPointerException if the given class name is null.
   */
  @Nullable RPCHandler handler(@NonNull String targetClassName);

  /**
   * Registers the given handler to this registry, replacing the handler which was registered previously for the target
   * class of the handler.
   *
   * @param rpcHandler the handler to register.
   * @return true if no handler was replaced to register the handler (clean insert), false otherwise.
   * @throws NullPointerException if the given rpc handler is null.
   */
  boolean registerHandler(@NonNull RPCHandler rpcHandler);

  /**
   * Unregisters the given rpc handler from this registry if previously registered.
   *
   * @param rpcHandler the handler to unregister.
   * @return true if the handler was removed from this registry, false otherwise.
   * @throws NullPointerException if the given rpc handler is null.
   */
  boolean unregisterHandler(@NonNull RPCHandler rpcHandler);

  /**
   * Unregisters the rpc handler associated with the given target class from this registry if registered previously.
   *
   * @param rpcHandlerTargetClass the class to unregister the rpc handler of.
   * @return true if the handler was removed from this registry, false otherwise.
   * @throws NullPointerException if the given target class is null.
   */
  boolean unregisterHandler(@NonNull Class<?> rpcHandlerTargetClass);

  /**
   * Unregisters the rpc handler associated with the given target class name from this registry if registered
   * previously.
   *
   * @param rpcHandlerTargetClassName the name of the class to unregister the rpc handler of.
   * @return true if the handler was removed from this registry, false otherwise.
   * @throws NullPointerException if the given target class name is null.
   */
  boolean unregisterHandler(@NonNull String rpcHandlerTargetClassName);

  /**
   * Unregisters all rpc handlers from this registry whose classes were loaded by the given class loader.
   *
   * @param classLoader the class loader to unregister the handlers based on.
   * @throws NullPointerException if the given class loader is null.
   */
  void unregisterHandlers(@NonNull ClassLoader classLoader);
}
