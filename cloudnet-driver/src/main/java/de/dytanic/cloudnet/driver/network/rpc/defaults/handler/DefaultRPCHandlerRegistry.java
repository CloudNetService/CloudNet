/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.rpc.defaults.handler;

import de.dytanic.cloudnet.driver.network.rpc.RPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandlerRegistry;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public class DefaultRPCHandlerRegistry implements RPCHandlerRegistry {

  protected final Map<String, RPCHandler> handlers = new ConcurrentHashMap<>();

  @Override
  public @NotNull @UnmodifiableView Map<String, RPCHandler> getRegisteredHandlers() {
    return Collections.unmodifiableMap(this.handlers);
  }

  @Override
  public boolean hasHandler(@NotNull Class<?> targetClass) {
    return this.hasHandler(targetClass.getName());
  }

  @Override
  public boolean hasHandler(@NotNull String targetClassName) {
    return this.handlers.containsKey(targetClassName);
  }

  @Override
  public @Nullable RPCHandler getHandler(@NotNull Class<?> targetClass) {
    return this.getHandler(targetClass.getName());
  }

  @Override
  public @Nullable RPCHandler getHandler(@NotNull String targetClassName) {
    return this.handlers.get(targetClassName);
  }

  @Override
  public boolean registerHandler(@NotNull RPCHandler rpcHandler) {
    return this.handlers.putIfAbsent(rpcHandler.getTargetClass().getName(), rpcHandler) == null;
  }

  @Override
  public boolean unregisterHandler(@NotNull RPCHandler rpcHandler) {
    return this.unregisterHandler(rpcHandler.getTargetClass());
  }

  @Override
  public boolean unregisterHandler(@NotNull Class<?> rpcHandlerTargetClass) {
    return this.unregisterHandler(rpcHandlerTargetClass.getName());
  }

  @Override
  public boolean unregisterHandler(@NotNull String rpcHandlerTargetClassName) {
    return this.handlers.remove(rpcHandlerTargetClassName) != null;
  }

  @Override
  public void unregisterHandlers(@NotNull ClassLoader classLoader) {
    for (Entry<String, RPCHandler> entry : this.handlers.entrySet()) {
      if (entry.getValue().getClass().getClassLoader().equals(classLoader)) {
        this.handlers.remove(entry.getKey(), entry.getValue());
      }
    }
  }
}
