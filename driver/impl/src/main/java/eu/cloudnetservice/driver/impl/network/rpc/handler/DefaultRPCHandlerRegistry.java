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

package eu.cloudnetservice.driver.impl.network.rpc.handler;

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandler;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of a rpc handler registry.
 *
 * @since 4.0
 */
@Singleton
@Provides(RPCHandlerRegistry.class)
public class DefaultRPCHandlerRegistry implements RPCHandlerRegistry {

  protected final Map<String, RPCHandler> handlers = new ConcurrentHashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull @UnmodifiableView Map<String, RPCHandler> registeredHandlers() {
    return Collections.unmodifiableMap(this.handlers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasHandler(@NonNull Class<?> targetClass) {
    return this.hasHandler(targetClass.getName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasHandler(@NonNull String targetClassName) {
    return this.handlers.containsKey(targetClassName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable RPCHandler handler(@NonNull Class<?> targetClass) {
    return this.handler(targetClass.getName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable RPCHandler handler(@NonNull String targetClassName) {
    return this.handlers.get(targetClassName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean registerHandler(@NonNull RPCHandler rpcHandler) {
    return this.handlers.put(rpcHandler.targetClass().getName(), rpcHandler) == null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean unregisterHandler(@NonNull RPCHandler rpcHandler) {
    // get the previous handler for the class and validate that they equal
    var handler = this.handler(rpcHandler.targetClass());
    if (handler == rpcHandler) {
      this.handlers.remove(handler.targetClass().getName());
      return true;
    }
    // the handlers did not match and was no unregistered
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean unregisterHandler(@NonNull Class<?> rpcHandlerTargetClass) {
    return this.unregisterHandler(rpcHandlerTargetClass.getName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean unregisterHandler(@NonNull String rpcHandlerTargetClassName) {
    return this.handlers.remove(rpcHandlerTargetClassName) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterHandlers(@NonNull ClassLoader classLoader) {
    for (var entry : this.handlers.entrySet()) {
      if (entry.getValue().getClass().getClassLoader().equals(classLoader)) {
        this.handlers.remove(entry.getKey(), entry.getValue());
      }
    }
  }
}
